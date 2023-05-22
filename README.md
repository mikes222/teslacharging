This app provides an easy way to control the charging features of your tesla. It is intended to being used in a smart home. 

## Features

Start and stop charging

Set the charging amps

Sets the charging limit in percent

Retrieve the charging info from the car

Opens/closes the chargeport

Set the charging limit in percent

Wake the car up

Automatically calculates the recommended charging amps based on the given power currently sent to the grid (positive values)/bought from the grid (negative values). 
The superflous power will be calculated to charge the car. Power already used to charge the car will be taken into account. 

## Compilation

As usual for java download *JDK* and *maven* and compile the code with

    mvn install
    
Grab the .jar file in the *target/* directory.

## Usage

Start the application with 

	java -jar teslacharging.jar -h
	
A short helpfile will be displayed

## Setup

	java -jar teslacharging.jar --auth step1

Execute the given URL in the browser, logon in the browser. When the browser shows "Page Not Found", copy the parameter "code" from the url and execute step 2 with it.

Step2 will save the necessary tokens to the configurationfile. The default location of the configurationfile is *"app.properties"*. 
This can be configured with the parameter *"--propertyfile <path_and_filename>"*. Note that the app needs write-access to the file.

Alternative to create a token:

https://github.com/fredli74/fetch-tesla-token

Grab it and enter the token in the configurationfile. 

## Special tipps

If you have more than one car enter the VIN of the car in the configurationfile. The rest will be filled out automatically.
You can use multiple propertyfiles for multiple cars.

## Contribution

Feel free to enhance the program as needed and create PullRequests so that others can benefit from your enhancements.

## Credits

**Source inspired by**

https://github.com/rrarey02/TeslaRTPCharging

**Tesla API documentation (unofficial):**

https://tesla-api.timdorr.com/

If you want to check out python and different usecases for tesla you may want to visit

https://github.com/strmotors/TeslaReport


## Disclaimer

We are not liable for any damage or any functionality of this app. The app is provided as-is. Use at your own risk.

## License

Apache-2.0 license
