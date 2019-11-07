#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import socket
import cv2
import numpy as np
import time
import uuid
HEAD = b'GgRd:'
HOST = '192.168.43.72'  # '192.168.43.72'  # '127.0.0.1'  # '49.233.153.135'
PORT = 9999

def tcp_main():
    # 创建tcp用和udp用socket:
    # socket_udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    # 获取本机MAC地址
    mac = uuid.UUID(int=uuid.getnode()).bytes[-6:]
    cap = cv2.VideoCapture(1)
    while True:
        # 开启摄像头捕获
        # time.sleep(1)
        ret, frame = cap.read()
        if ret:
            # 构造图像帧： 帧内容为 帧头('GgRdI:') + 帧长度(4字节) + 状态码(0x00) + 本机的MAC地址(6字节) + 压缩后的图片
            frame_in_bytes = b'\x00' + mac + np.array(cv2.imencode('.jpg', frame)[1]).tostring()
            frame_len: int = len(frame_in_bytes)
            data = HEAD + frame_len.to_bytes(4, 'little') + frame_in_bytes
            socket_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            socket_tcp.connect((HOST, PORT))
            # 将帧拆成若干长度为1024的片段发出
            # str = b"various"
            # len_str = len(str)
            # socket_tcp.send(b"GgRd:" + len_str.to_bytes(4, 'big') + str)
            while len(data):
                socket_tcp.send(data[:1024])
                data = data[1024:]
            socket_tcp.close()
        # 关闭摄像头捕获


def udp_main():
    capture_freq = 0
    # 创建tcp用和udp用socket:
    # socket_udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    # 获取本机MAC地址
    mac = uuid.UUID(int=uuid.getnode()).bytes[-6:]
    cap = cv2.VideoCapture(1)

    while True:
        # 开启摄像头捕获
        # time.sleep(1)
        ret, frame = cap.read()
        if ret:
            # 构造图像帧： 帧内容为 帧头('GgRdI:') + 帧长度(4字节) + 状态码(0x00) + 本机的MAC地址(6字节) + 压缩后的图片
            socket_udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            frame_in_bytes = b'\x00' + mac + np.array(cv2.imencode('.jpg', frame)[1]).tostring()
            frame_len: int = len(frame_in_bytes)
            data = HEAD + frame_len.to_bytes(4, 'little') + frame_in_bytes
            while len(data):
                socket_udp.sendto(data[:1024], (HOST, PORT))
                data = data[1024:]
            socket_udp.close()
        # 关闭摄像头捕获


if __name__ == '__main__':
    tcp_main()
