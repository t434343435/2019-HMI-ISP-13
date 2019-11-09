# 2019-HMI-ISP-13
## 基于OpenCV的表盘识别与监控系统设计

### 项目结构
- 2019-HMI-ISP-13
该目录是由Android Studio创建的程序，其结构为
------------
2019-HMI-ISP-13/app/src/main/java/com/swr/gauge_reader 存储了安卓程序的源代码
2019-HMI-ISP-13/app/src/main/res/ 存储了安卓程序的资源文件，如图标、布局等

- 2019-HMI-ISP-13/python程序
该目录存储了摄像头客户端和图像处理的服务器端程序
------------
    readerclient.py 文件为摄像头客户端程序
    process_server.py 为图像处理的服务器端程序
    gauge_reader.py 为仪表识别算法

- 2019-HMI-ISP-13/报告
该目录存储了该项目的开题报告和结题报告及演示视频
------------
    设置模板.mp4 视频为对监控环境的查看，并通过设置模板到服务器，以获得仪表中心和指针长度供服务器识别
	波形查看.mp4 视频为查看服务器发送来的识别结果，并显示在手机屏幕上，通过人与手机的人机交互，可以对波形进行缩放拖动复原查看数据等操作
	波形保存.mp4 视频为对上述波形进行保存，重现，删除，并可通过选项改变显示界面。
	
	
### 使用方法
- 图像处理服务端
修改 python程序/process_server.py 中的HOST,TCP_PORT为绑定的服务器地址和端口即可
- 摄像头客户端
修改 python程序/readerclient.py 中的HOST,PORT为访问的服务器地址和端口即可
- 安卓服务端
修改 app/src/main/java/com/swr/gauge_reader/InternetTCPService.java中InternetTCPService类中SERVER_HOST_IP，SERVER_HOST_PORT为服务器地址端口
