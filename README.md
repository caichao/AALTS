# AALTS: Asynchronous Acoustic Localization and Tracking for Mobile Targets


## This respository contains the source code for our android side app (Java and c based), beacon side app (c oriented), and server side app (Java).

1. The file named "platform_cc_single_transmission_neon_ten_tones.zip
" is the beacon side code. 
2. The file named "server_side_application.zip" is for server side application.
3. All other files are for Android side application. 


## To run AALTS, it needs to run the three applications in tandem. 

1. First, run the server side application

2. Second, activate the beacon side program
This localization project needs at least three beacons for support. For more information about the beacon, please visit our [ASDR platform](https://github.com/caichao/ASDR). We wrote a json script to configure these beacons. This json script allows to write the following 
'''
{
"server_ip":"192.168.1.145", 
"schedule_port":"22222",
"upload_port":"33333",
"anchorId":"2",
"preamble_threshold":"6"
}
'''

3. Open the android side app. 

4. You can now see the located positions on the server side. 
