const videoElement = document.getElementById('video');
const startAVButton = document.getElementById('startAV');
const stopAVButton = document.getElementById('stopAV');

let mediaStream = null;
let audioRecorder = null;
let videoRecorder = null;
let socket = null;

var audioContext;

startAVButton.addEventListener('click', async () => {
    try {
        // Request access to both video and audio with full quality
        mediaStream = await navigator.mediaDevices.getUserMedia({
            audio: true,
            video: {
                width: { ideal: 640 }, // Ideal width in pixels
                height: { ideal: 480 }, // Ideal height in pixels
                frameRate: { ideal: 15 } // Ideal frame rate in frames per second
            }
        });

        // Display video stream in the video element
        videoElement.srcObject = mediaStream;
        date = new Date().toISOString();
        console.log(date)

        // Initialize WebSocket
        socket = new WebSocket("ws://localhost:8080/stream?sessionDateTime=" + encodeURIComponent(date));
        socket.binaryType = "arraybuffer";
        socket.onopen = () => {
            console.log("WebSocket connection opened.");
        };
        socket.onerror = (error) => {
            console.error("WebSocket error:", error);
        };
        socket.onclose = (event) => {
            console.log("WebSocket connection closed:", event.code, event.reason);
        };

        socket.onmessage = function(event) {
            console.log("testing")
            if (typeof event.data === 'string') {
                // Parse the JSON message
                var message = JSON.parse(event.data);
                if (message.type === 'audio_output') {
                    // Extract the base64-encoded audio data
                    var base64Data = message.data;

                    // Decode the base64 string
                    var audioData = base64ToArrayBuffer(base64Data);

                    // Play the audio
                    playAudio(audioData);
                } else if (message.type === 'backend_message') {
                      console.log("Overall Feedback Received:", message);
                      console.log("Video Link:", message.videoLink);

                      // Optionally, display the video link in the UI
                      // ...
                }  else {
                    // Handle other message types if necessary
                    console.log("Received message:", message);
                }
            }
        };

        // Create separate tracks for audio and video

        // Create audio recorder
        const audioStream = new MediaStream([mediaStream.getAudioTracks()[0]]);
        audioRecorder = new MediaRecorder(audioStream);

        audioRecorder.ondataavailable = (event) => {
            if (event.data.size > 0 && socket && socket.readyState === WebSocket.OPEN) {
                event.data.arrayBuffer().then((arrayBuffer) => {
                    const messageType = new Uint8Array([0x01]); // Audio message type
                    const messageData = new Uint8Array(arrayBuffer);
                    const message = new Uint8Array(messageType.length + messageData.length);
                    message.set(messageType);
                    message.set(messageData, messageType.length);
                    socket.send(message.buffer);
//                    console.log("Audio chunk sent:", messageData.length, "bytes");
                });
            }
        };

        // Create video recorder
        const videoStream = new MediaStream([mediaStream.getVideoTracks()[0]]);
        const videoOptions = {
            mimeType: 'video/webm; codecs=vp8'
        };
        videoRecorder = new MediaRecorder(videoStream, videoOptions);

        videoRecorder.ondataavailable = (event) => {
            if (event.data.size > 0 && socket && socket.readyState === WebSocket.OPEN) {
                event.data.arrayBuffer().then((arrayBuffer) => {
                    const messageType = new Uint8Array([0x02]); // Video message type
                    const messageData = new Uint8Array(arrayBuffer);
                    const message = new Uint8Array(messageType.length + messageData.length);
                    message.set(messageType);
                    message.set(messageData, messageType.length);
                    socket.send(message.buffer);
//                    console.log("Video chunk sent:", messageData.length, "bytes");
                });
            }
        };

        // Start recording with a timeslice (in ms) for continuous data chunks
        audioRecorder.start(100); // Adjust timeslice as needed
        videoRecorder.start(100); // Adjust timeslice as needed

        // Update button states
        startAVButton.disabled = true;
        stopAVButton.disabled = false;
        console.log("Audio and Video recording started.");
    } catch (error) {
        console.error("Error accessing media devices.", error);
    }
});

stopAVButton.addEventListener('click', () => {
    // Stop the recorders
    if (audioRecorder && audioRecorder.state !== 'inactive') {
        audioRecorder.stop();
    }
    if (videoRecorder && videoRecorder.state !== 'inactive') {
        videoRecorder.stop();
    }
    // Stop all media tracks
    if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
    }
    videoElement.srcObject = null;

    if (socket && socket.readyState === WebSocket.OPEN) {
//        socket.close();
        const messageType = new Uint8Array([0x04]); // Ending signal type
        socket.send(messageType.buffer);
        console.log("sent 0x04 ending signal to backend");
    }

    // Update button states
    startAVButton.disabled = false;
    stopAVButton.disabled = true;
    console.log("Audio and Video recording stopped.");
});



// Helper function to decode base64 to ArrayBuffer
function base64ToArrayBuffer(base64) {
    var binaryString = atob(base64);
    var len = binaryString.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

var audioQueue = [];
var isPlaying = false;

function playAudio(arrayBuffer) {
    // Add the audio data to the queue
    audioQueue.push(arrayBuffer);

    // If nothing is currently playing, start playing the next audio in the queue
    if (!isPlaying) {
        playNextAudio();
    }
}

function playNextAudio() {
    if (audioQueue.length === 0) {
        isPlaying = false;
        return;
    }

    var arrayBuffer = audioQueue.shift(); // Get the next audio data from the queue
    isPlaying = true;

    audioContext.decodeAudioData(arrayBuffer, function(buffer) {
        var source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(audioContext.destination);
        source.start(0);

        // When the audio finishes playing, try to play the next one
        source.onended = function() {
            isPlaying = false;
            playNextAudio();
        };
    }, function(e) {
        console.error("Error decoding audio data:", e);
        // Ensure that even if there's an error, we attempt to play the next audio
        isPlaying = false;
        playNextAudio();
    });
}

// ------------------ Audio Playback Functions ------------------

// Initialize Audio Context
function initAudioContext() {
    if (!audioContext) {
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
    }
}

// Add a message to the messages div
function addMessage(sender, message) {
    const messageDiv = document.createElement('div');
    messageDiv.classList.add('message');
    messageDiv.innerHTML = `<strong>${sender}:</strong> ${message}`;
    messagesDiv.appendChild(messageDiv);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

// Function to play received audio
function playAudio(arrayBuffer) {
    initAudioContext();

    // Add the audio data to the queue
    audioQueue.push(arrayBuffer);

    // If nothing is currently playing, start playing the next audio in the queue
    if (!isPlaying) {
        playNextAudio();
    }
}

// Function to play the next audio in the queue
function playNextAudio() {
    if (audioQueue.length === 0) {
    isPlaying = false;
    return;
}

isPlaying = true;
const arrayBuffer = audioQueue.shift();

audioContext.decodeAudioData(arrayBuffer)
    .then(buffer => {
        const source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(audioContext.destination);
        source.start(0);

        // When the audio finishes playing, play the next one
        source.onended = () => {
        isPlaying = false;
        playNextAudio();
        };
    })
    .catch(e => {
        console.error("Error decoding audio data:", e);
        isPlaying = false;
        playNextAudio();
    });
}

// send screenshot every 600ms
const canvasElement = document.createElement('canvas');
const context = canvasElement.getContext('2d');


const sendScreenshot = () => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    canvasElement.width = videoElement.videoWidth;
    canvasElement.height = videoElement.videoHeight;
    // Draw the current video frame to the canvas
    context.drawImage(videoElement, 0, 0, canvasElement.width, canvasElement.height);

    // Get the image data as a Blob
    canvasElement.toBlob((blob) => {
      if (blob) { // Ensure blob is valid
        const reader = new FileReader();
        reader.onload = function() {
          const arrayBuffer = this.result;
          const messageType = new Uint8Array([0x03]); // Screenshot message type
          const messageData = new Uint8Array(arrayBuffer);
          const message = new Uint8Array(messageType.length + messageData.length);
          message.set(messageType);
          message.set(messageData, messageType.length);
          socket.send(message.buffer);
        };
        reader.readAsArrayBuffer(blob);
      } else {
        console.error("Failed to create blob from canvas.");
      }
    }, 'image/jpeg'); // Specify image format
  }
};

// Start sending screenshots every 600ms
let screenshotInterval = setInterval(sendScreenshot, 600);

// Make sure to clear the interval when stopping the video
stopAVButton.addEventListener('click', () => {
    clearInterval(screenshotInterval);
});
