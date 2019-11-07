#!/usr/bin/env python3
# -*- coding: utf-8 -*-
FIND_HEAD = 0
READ_LEN = 1
READ_DATA = 2


class PackAnalyzer:

    def __init__(self, head):
        self.__state = FIND_HEAD
        self.__bytes_buffer = b''
        self.__data_buffer = b''
        self.__data_len = 0
        self.__received_len = 0
        self.pack_head = head

    def get_pack(self, buff):
        data = self.__bytes_buffer + buff
        # 状态机 获取帧头后，将帧传送给handle_udp_data(data)处理
        self.__bytes_buffer = b'\0'
        while len(self.__bytes_buffer):
            if self.__state == FIND_HEAD:
                index = data.find(self.pack_head)
                if index <= -1:
                    self.__bytes_buffer = b''
                    self.__state = FIND_HEAD
                    return False, b''
                else:
                    self.__bytes_buffer = data[index + len(self.pack_head):]
                    data = data[index + len(self.pack_head):]
                    self.__state = READ_LEN

            elif self.__state == READ_LEN:
                self.__data_len = int.from_bytes(data[:4], 'little')
                self.__bytes_buffer = data[4:]
                data = data[4:]
                self.__data_buffer = b''
                self.__state = READ_DATA
                self.__received_len = 0

            elif self.__state == READ_DATA:
                self.__received_len = self.__received_len + len(data)
                if self.__received_len < self.__data_len:
                    self.__bytes_buffer = b''
                    self.__data_buffer = self.__data_buffer + data
                    self.__state = READ_DATA
                    return False, b''
                else:
                    self.__bytes_buffer = data[self.__data_len - self.__received_len + len(data):]
                    self.__data_buffer = self.__data_buffer + data[:self.__data_len - self.__received_len + len(data)]
                    self.__state = FIND_HEAD
                    self.__data_len = 0
                    self.__received_len = 0
                    return True, self.__data_buffer
