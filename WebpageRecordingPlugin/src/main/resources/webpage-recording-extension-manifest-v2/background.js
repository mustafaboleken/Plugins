"use strict";

import {WebRTCAdaptor} from "./js/webrtc_adaptor.js"

let antMediaState = {
    READY: "ready",
    WAITING: "waiting",
    STARTED: "started",
    FINISHED: "finished",
    ERROR: "error"
};

chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
    console.log('GOT EXTERNAL MESSAGE', message);

    let key = "webRTCAdaptorState";
    let errorMessageKey = "webRTCAdaptorError";

    let webRTCAdaptorStateCurrent = localStorage.getItem(key);
    if (webRTCAdaptorStateCurrent == null || webRTCAdaptorStateCurrent == undefined) {
        webRTCAdaptorStateCurrent = antMediaState.READY;
    }
    let webRTCAdaptorErrorCurrent = localStorage.getItem(errorMessageKey);
    if (webRTCAdaptorErrorCurrent == null || webRTCAdaptorErrorCurrent == undefined) {
        webRTCAdaptorErrorCurrent = "";
    }

    sendResponse({"streamId": message.streamId, "webRTCAdaptorState": webRTCAdaptorStateCurrent, "webRTCAdaptorError": webRTCAdaptorErrorCurrent});
});

function getVideoAudioStream(options) {
    return new Promise((resolve, reject) => {
        chrome.tabCapture.capture(options, stream => {
            resolve(stream);
        });
    });
}

chrome.runtime.onConnect.addListener(port => {
    let webRTCAdaptor;

    let mediaConstraints;

    port.onMessage.addListener(async (message) => {
        console.log('GOT MESSAGE', message);

        mediaConstraints = {
            video : true,
            audio : true,
            videoConstraints : {
                mandatory : {
                    chromeMediaSource : 'tab',
                    minFrameRate: 4,
                    maxFrameRate: 20,
                    maxWidth: 1920,
                    maxHeight: 1280,
                    minWidth: 640,
                    minHeight: 480
                }
            }
        };

        /*if (message.width > 0 && message.height > 0) {
            mediaConstraints = {
                video : true,
                audio : true,
                videoConstraints : {
                    mandatory : {
                        chromeMediaSource : 'tab',
                        minFrameRate: 4,
                        maxFrameRate: 20,
                        maxWidth : message.width,
                        maxHeight : message.height,
                        minWidth : message.width,
                        minHeight : message.height
                    }
                }
            };
        } else {
            mediaConstraints = {
                video : true,
                audio : true,
                videoConstraints : {
                    mandatory : {
                        chromeMediaSource : 'tab',
                        minFrameRate: 4,
                        maxFrameRate: 20
                    }
                }
            };
        }
        */


        if (message.command === 'WR_START_BROADCASTING') {
            let key = "webRTCAdaptorState";

            localStorage.setItem(key, antMediaState.WAITING);     
            let token = message.token;

            if (token == null || token == undefined) {
                token = "";
            }

            const stream = await getVideoAudioStream(mediaConstraints);

            const track = stream.getVideoTracks()[0];

            const constra = {
                width: { min: 640, ideal: 1280 },
                height: { min: 480, ideal: 720 },
                advanced: [{ width: 1920, height: 1280 }, { aspectRatio: 1.333 }],
                resizeMode: 'crop-and-scale'
            };

            track.applyConstraints(constra);

            webRTCAdaptor = new WebRTCAdaptor({
                websocket_url : message.websocketURL,
                mediaConstraints : mediaConstraints,
                localStream: stream,
                callback : (info, obj) => {
                    if (info == "initialized") {
                        webRTCAdaptor.publish(message.streamId, token, "", "", "", "");
                    } else if (info == "publish_started") {
                        console.log("mediapush_publish_started");
                        localStorage.setItem(key, antMediaState.STARTED);
                    }
                    console.log(info);
                },
                callbackError : function(error, message) {
                    var errorMessage = JSON.stringify(error);
                    if (typeof message != "undefined") {
                        errorMessage = message;
                    } else {
                        errorMessage = JSON.stringify(error);
                    }
        
                    if (error.indexOf("WebSocketNotConnected") != -1) {
                        errorMessage = "WebSocket is disconnected.";
                    } else if (error.indexOf("not_initialized_yet") != -1) {
                        errorMessage = "Server is getting initialized.";
                    } else if (error.indexOf("data_store_not_available") != -1) {
                        errorMessage = "Data store is not available. It's likely that server is initialized or getting closed";
                    } else {
                        if (error.indexOf("NotFoundError") != -1) {
                            errorMessage = "Camera or Mic are not found or not allowed in your device";
                        } else if (error.indexOf("NotReadableError") != -1 || error.indexOf("TrackStartError") != -1) {
                            errorMessage = "Camera or Mic are already in use and they cannot be opened. Choose another video/audio source if you have on the page below ";
        
                        } else if (error.indexOf("OverconstrainedError") != -1 || error.indexOf("ConstraintNotSatisfiedError") != -1) {
                            errorMessage = "There is no device found that fits your video and audio constraints. You may change video and audio constraints"
                        } else if (error.indexOf("NotAllowedError") != -1 || error.indexOf("PermissionDeniedError") != -1) {
                            errorMessage = "You are not allowed to access camera and mic.";
                        } else if (error.indexOf("TypeError") != -1) {
                            errorMessage = "Video/Audio is required";
                        } else if (error.indexOf("getUserMediaIsNotAllowed") != -1) {
                            errorMessage = "You are not allowed to reach devices from an insecure origin, please enable ssl";
                        } else if (error.indexOf("ScreenSharePermissionDenied") != -1) {
                            errorMessage = "You are not allowed to access screen share";
                            $(".video-source").first().prop("checked", true);
                        } else if (error.indexOf("UnsecureContext") != -1) {
                            errorMessage = "Please Install SSL(https). Camera and mic cannot be opened because of unsecure context. ";
                        }
                        else if (error.indexOf('no_stream_exist') != -1) {
                            errorMessage = 'There is no active live stream with this id to play';
                        } else {
                            errorMessage = error
                        }
                    }
                    if (message !== undefined) {
                        console.log("error callback: " + error + " message: " + errorMessage);
                    }
        
                    let errorMessageKey = "webRTCAdaptorError";
                    localStorage.setItem(errorMessageKey, errorMessage);
                    localStorage.setItem(key, antMediaState.ERROR);
                }
            });
        }
        else if (message.command === 'WR_STOP_BROADCASTING') {
            let key = "webRTCAdaptorState";
            localStorage.setItem(key, antMediaState.FINISHED);
        }
        else if (message.command === 'WR_GET_STATE') {
            let key = "webRTCAdaptorState";
            let errorMessageKey = "webRTCAdaptorError";

            chrome.storage.sync.get(["webRTCAdaptorState", "webRTCAdaptorError"], function(items) {
                console.log('State & latest error message retrieved', items);
                port.postMessage({
                    command: 'WR_STATE',
                    state: items[0],
                    error: items[1]
                });
              });
        }
    });
});

