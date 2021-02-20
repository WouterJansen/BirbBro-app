
# BirdBro-app

BirdBro is a Android application to receive notifications of your friendly bird friends visiting your birdfeeder.
This app is connected over Google Firebase cloud to a internet-connected ESP32 camera system inside the birdfeeder to capture images and upload them to the app. 

You can find more information on the ESP32 camera system here: https://github.com/WouterJansen/BirbBro-esp32

##  Features

  - Receive notifications when a new picture is taken
  - Browse all previously taken photos of your feathered friends.


##  TODO

  - Create ML model that recognizes the bird species and shows it.
  
##  Usage

  - The app requires a `google_services.json` file within the app-folder to work. 
  - The location of the images is hardcoded on the Firebase storage bucket to be in a main folder as `.jpg` images and requires `read` and `write`  on all those files. The image names need to be epoch timestamps. 
