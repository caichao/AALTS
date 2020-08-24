# AALTS: Asynchronous Acoustic Localization and Tracking for Mobile Targets


## This respository contains the source code for our android side app (Java and c based), beacon side app (c oriented), and server side app (Java).

1. The file named "platform_cc_single_transmission_neon_ten_tones.zip
" is the beacon side code. 
2. The file named "server_side_application.zip" is for server side application.
3. All other files are for Android side application. 


## To run AALTS, it needs to run the three applications in tandem. 
This localization project needs at least three beacons for support. For more information about the beacon, please visit our [ASDR platform](https://github.com/caichao/ASDR). The beacons should be distributed in a room and connect to a wireless hot spot. 

1. First, run the server side application
Here, we do not provide any clickable and executable program. So you need to either run using cmd command lines or IDEA. 
After running the program of server side, you should configure the anchor positions. Then the next step is for anchor configurations. 
![anchor position configuration](https://github.com/caichao/AALTS/blob/master/images/server_configurations.webp)

2. Second, connect the beacon to a hotspot. You can wrote a bash for automatic connection. 
If it is possible, you'd better bind a static IP for each beacon. This can be achieved by encoding a relationship between the MAC address of pi and a static ip. 

We wrote a json script to configure these beacons. 
'''
{
"server_ip":"192.168.1.145", 
"schedule_port":"22222",
"upload_port":"33333",
"anchorId":"2",
"preamble_threshold":"6"
}
'''
the server_ip indicates your server side host ip;
the sechdule_port is to receive the server side app's broadcasting message for schedule beacon message transmission;
the upload_port is used to upload detect beacon messages;
the anchorId is for current beacon and it should be unique;
the preamble_threshold should be calibrate but only needs one-time efforts. 

After your successfully connect the pi to a wireless hotspot. Then you can run the bash file (launch_util_gpu.sh) we provided on your PC side to schedule the beacons for message transmissions. The following steps are for windows.
[1] Open git bash at the folder where it contains the shell script;
[2] ''' source launch_util_gpu.sh'''
[3] ''' lanchAll'''
Then, everything is ready now. 
![step to run the shell script](https://github.com/caichao/AALTS/blob/master/images/lanch.webp)

Before the above preparation, you can first run the beacon side program solely on the pi so as to check whether the acoustic board function properly. 
If the acoustic board is ok, you can see the following logging information. 
![Log message is the board is ok](https://github.com/caichao/AALTS/blob/master/images/anchor_debug.webp)


3. Open the android side app. 
At the first time you run this application, you should configure the server side IP, port, and target id. There is also another page which can be activated on the menu bar to debug algorithm parameters. For inexperience user, we do not recommend any modification. 
Please DO ALLOW recording when first install this app. 
![running ui](https://github.com/caichao/AALTS/blob/master/images/run_ui.png), ![configuration page](https://github.com/caichao/AALTS/blob/master/images/conf.png)


4. You can now see the located positions on the server side. 
![Running AALTS](https://github.com/caichao/AALTS/blob/master/images/runtime_ui.png)

Developer: Chao Cai (caichao08@gmail), Ruinan Jin (jrnsneepy@gmail.com), Yan Yan (1063422772@qq.com)
Experimenter: Peng Wang (somewap@qq.com), Liyuan Ye (yeliyuan1997@qq.com), Pei Rao (raopei1994@163.com)

If you find this helpful, please cite our paper, thanks. 
@ARTICLE{AALTS,
  author={C. {Cai} and R. {Zheng} and J. {Li} and L. {Zhu} and H. {Pu} and M. {Hu}},
  journal={IEEE Internet of Things Journal}, 
  title={Asynchronous Acoustic Localization and Tracking for Mobile Targets}, 
  year={2020},
  volume={7},
  number={2},
  pages={830-845},}
  
 
