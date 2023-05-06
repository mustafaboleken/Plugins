# WebpageRecordingPlugin

Webpage Recording Plugin can stream everything on the given web page with video and audio in realtime.

## Features

### 1. Broadcast the whole web page

You can broadcast the whole web page with video and audio in realtime.

### 2. Record the broadcast if needed

You can record the broadcast if needed. But you need to start the recording manually with REST Api or on Ant Media Server Dashboard.

## How to Install

### Install Google Chrome 112

Webpage Recording Plugin uses Google Chrome 112 to broadcast the web page. So you need to install Google Chrome 112 to your server. You can install it on Ubuntu with the following commands.


    ```
    wget --no-verbose -O /tmp/chrome.deb https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_112.0.5615.165-1_amd64.deb
    ```

    ```
    sudo apt install -y /tmp/chrome.deb
    ```

    ```
    rm /tmp/chrome.deb
    ```

### Clone the repository

    ```
    git clone https://github.com/ant-media/Plugins
    ```

### Go to the Webpage Recording Plugin directory

    ```
    cd Plugins/WebpageRecordingPlugin
    ```

### Modify the redeploy.sh file with your Ant Media Server installation path

    ```
    Change AMS_DIR=/usr/local/antmedia/
    ```

### Build & install the plugin

    ```
    chmode +x redeploy.sh
    ./redeploy.sh
    ```

### Restart Ant Media Server

    ```
    sudo service antmedia restart
    ```

## How to Use

Webpage Recording Plugin have REST API to control the plugin. 

### 1. Start the broadcast

Call the REST Method below to let Ant Media Server broadcast the web page. You should pass the url of the web page and streamId you wanted to use as a parameter.
   ```
   curl -i -X POST -H "Accept: Application/json" -H "Content-Type: application/json" "http://localhost:5080/WebRTCAppEE/rest/v2/webpage/record/start" -d '{"streamId":"streamId","webpageUrl": "http://example.com"}'
   ```

### 2. Stop the broadcast

Call the REST Method below to let Ant Media Server with the stream id you specified in the start method.
   ```
   curl -i -X GET -H "Accept: Application/json" "http://localhost:5080/WebRTCAppEE/rest/v2/webpage/record/stop/{streamId}"
   ```

### Customization
Other than that, you can modify the code and build the plugin by yourself to make it work with your own needs. For example, you can play the video or login to the web page with your own credentials before starting the broadcast.
Go to the WebpageRecordingPlugin and modify the customModification method as you wish. Then build the plugin with the following command.

   ```
   chmode +x redeploy.sh
   ./redeploy.sh
   ```