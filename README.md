# MoodTrack Android

MoodTrack Android is an experimental prototype for gathering user input through notifications. This app serves as the user-facing client for serving content and triggering events through notifications as well as questionnaires. 

## Worth getting familiar with before starting
- [Dagger/Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room](https://developer.android.com/training/data-storage/room)
- [Apollo Android](https://www.apollographql.com/docs/android/)
- [Firebase](https://firebase.google.com/docs/android/setup)

## Miniumum Requirements
The project's ```minSdkVersion``` is set to ```23```, but it may be possible to further reduce the SDK level thanks for API desugaring.

## App Signing
Signing credentials are loaded from a ```keystore.properties``` file which should be located in your project root. 
````
// gradle.build
def keystorePropertiesFile = rootProject.file("keystore.properties")  
def keystoreProperties = new Properties();  
keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
````

Define your credentials in the keystore file in order to build from the command line, and have the ```storefile``` variable point to your Java keystore  (.jks) location: 
````
// keystore.properties
storePassword=
keyPassword=
keyAlias=
storeFile=
````

## Build and Install
If properly configured, the code required for Android Apollo should be generated when the Gradle build process is started. 

Before building, a ```keystore.properties``` and a ```config.properties```must be created in the project root. For development purposes, ```config.properties``` should at least contain the URL to your local API:
````
deploymentUrl="https://your-url-here/graphql"
developmentUrl="http://10.0.2.2:4000/graphql"
````

For development purposes, the URL to the API can also be defined directly in the app-level ```gradle.build``` file. To customize this, replace ```<port>``` and ```<endpoint>``` with your API port and endpoint. 
````
debug {  
    manifestPlaceholders = [clearTextEnabled: "true"]  
    debuggable true  
    signingConfig signingConfigs.debug  
    buildConfigField "String", "SERVER_URL", '"http://10.0.2.2:<port>/<endpoint>"'  
}
````

For deployment purposes, the ```SERVER_URL``` field should point to your API endpoint in your ```release``` build type:
`````
release {  
    manifestPlaceholders = [clearTextEnabled:  "false"]  
    signingConfig signingConfigs.release  
    buildConfigField "String", "SERVER_URL", '"http://your-api/endpoint"'  
}
`````

If you defined your server URL in ``config.properties```, ``ou can instead just refer to the config file contents:

````
release {
    manifestPlaceholders = [clearTextEnabled:  "false"]
    signingConfig signingConfigs.release
    buildConfigField "String", "SERVER_URL", configProperties['deploymentUrl']
}
````
 
 A Firebase project with [Firebase Authentication](https://firebase.google.com/docs/auth) is needed in order to enable authentication features. In order to use different Firebase projects for different releases, place your ```google-services.json``` in a folder under ```app/src``` which corresponds to your build type (for instance ```app/src/debug/google-services.json``` for a development environment with the name ```debug```).
