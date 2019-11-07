#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import socket
import time
import uuid
import socketserver
from pack_analyzer import *
from gauge_reader import *

HEAD = b'GgRd:'
HOST = '192.168.43.72'  # '0.0.0.0'  # '192.168.43.72'  # '127.0.0.1'  # '106.54.219.89'
TCP_PORT = 9999
UDP_PORT = 9998
HOUR = 3600

img_coded = np.array(cv2.imencode('.jpg', cv2.imread('yibiao.png'))[1]).tostring()
gauge_list = []
angles = []


def handle_tcp_pack(socket_tcp, data):
    global img_coded, gauge_list,angles
    code = data[0]  # 获取状态码
    if code == 0x00:  # 0x00为图像传输状态码
        mac_bytes = data[1:7].hex()
        mac = ":".join([mac_bytes[e:e + 2] for e in range(0, 11, 2)])  # 发送的客户端的MAC地址
        img_coded = data[7:]  #
        if len(gauge_list):
            img_decode = cv2.imdecode(np.fromstring(data[7:], dtype=np.uint8), cv2.IMREAD_COLOR)  # 图像解码
            i = 0
            for gr in gauge_list:
                angle = gr.find_angle(img_decode)
                # print(angle)
                if angle is not None:
                    angles[i] = angle
                i = i + 1

    if code == 0x80:  # 0x80为数据传输状态码
        num = len(angles)
        str_head = num.to_bytes(4, 'little')
        for i in range(num):
            str_head = str_head + np.array(angles[i], dtype=np.float64).tobytes()
        str_head = b"\x80" + str_head
        len_str = len(str_head)
        socket_tcp.send(b"GgRd:" + len_str.to_bytes(4, 'little') + str_head)

    if code == 0x81:  # 0x81为图像传输状态码
        if img_coded is not None:
            num = len(angles)
            str_head = num.to_bytes(4, 'little')
            for i in range(num):
                str_head = str_head + np.array(angles[i], dtype=np.float64).tobytes()
            mac = uuid.UUID(int=uuid.getnode()).bytes[-6:]
            frame_in_bytes = b'\x81' + str_head + mac + img_coded
            frame_len = len(frame_in_bytes)
            data = HEAD + frame_len.to_bytes(4, 'little') + frame_in_bytes
            while len(data):
                socket_tcp.send(data[:1024])
                data = data[1024:]

    if code == 0x82:  # 0x82 设置模板
        gauge_num = np.frombuffer(data[1:5], dtype=np.int32)
        for i in range(gauge_num[0]):
            nums = np.frombuffer(data[5 + 48 * i: 53 + 48 * i], dtype=np.int32)
            gr = GaugeReader(((nums[0], nums[1]), (nums[2], nums[3])), ((nums[4], nums[5]), (nums[6], nums[7])),
                             ((nums[8], nums[9]), (nums[10], nums[11])), 15)
            gauge_list.append(gr)

        angles = [0.0] * gauge_num[0]
        bytes_to_send = HEAD + b'\x01\x00\x00\x00\x82'
        socket_tcp.send(bytes_to_send)

    if code == 0x83:
        gauge_list = []
        angles = []
        bytes_to_send = HEAD + b'\x01\x00\x00\x00\x83'
        print(gauge_list)
        socket_tcp.send(bytes_to_send)


class GaugeReaderTCPServer(socketserver.StreamRequestHandler):
    def setup(self):
        pass

    def handle(self):
        super().handle()
        pa = PackAnalyzer(HEAD)
        while True:
            try:
                data = self.request.recv(1024)
            except ConnectionResetError:
                print('raise ConnectionResetError')
                pass
            else:
                res, pack = pa.get_pack(data)
                if res:
                    handle_tcp_pack(self.request, pack)
                    return

    def finish(self):
        pass



class GaugeReaderUDPServer(socketserver.DatagramRequestHandler):
    pa_dic = dict()

    def setup(self):
        super().setup()

    def handle(self):
        super().handle()
        data = self.request[0]
        if data[:len(HEAD)] == HEAD:
            self.pa_dic[self.client_address] = (time.time(), PackAnalyzer(HEAD))
        if self.pa_dic.__contains__(self.client_address):
            pa = self.pa_dic[self.client_address][1]
            res, pack = pa.get_pack(data)
            if res:
                handle_udp_pack(self.request[1], pack)
                self.pa_dic.pop(self.client_address)
        now = time.time()
        for key in list(self.pa_dic.keys()):
            if now - self.pa_dic[key][0] > 1:
                self.pa_dic.pop(key)

    def finish(self):
        pass


if __name__ == '__main__':
    server_tcp = socketserver.ThreadingTCPServer((HOST, TCP_PORT), GaugeReaderTCPServer)  # 多线程版
    server_tcp.serve_forever()
