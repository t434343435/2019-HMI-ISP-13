import cv2
import numpy as np
import math
from matplotlib import pyplot as plt

ITERATION = 150

region = [(-1, -1), (-1, -1)]
min_vec = [(-1, -1), (-1, -1)]
max_vec = [(-1, -1), (-1, -1)]
count = 0

frame_buffer = np.zeros((512, 512, 3), np.uint8)
cap_frame = np.zeros((512, 512, 3), np.uint8)

angle_list = []


def draw_region(event, x, y, flags, param):
    global region, count, frame_buffer, cap_frame
    if event == cv2.EVENT_LBUTTONUP:
        region[count] = (x, y)
        count = count + 1
        if count >= 2:
            count = 0
    frame_buffer = cap_frame
    if count == 1:
        cv2.rectangle(frame_buffer, region[0], region[0], (255, 0, 0), 3)
    if count == 0:
        cv2.rectangle(frame_buffer, region[0], region[1], (255, 0, 0), 3)


def draw_vec(event, x, y, flags, param):
    global region, count, frame_buffer, cap_frame
    if event == cv2.EVENT_LBUTTONUP:
        region[count] = (x, y)
        count = count + 1
        if count >= 2:
            count = 0
    frame_buffer = cap_frame.copy()
    if count == 1:
        cv2.line(frame_buffer, region[0], region[0], (255, 0, 0), 3)
    if count == 0:
        cv2.line(frame_buffer, region[0], region[1], (255, 0, 0), 3)


def length(x1, x2, y1, y2):
    return math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2)


def dot2linedistance(dot, line_dot1, line_dot2):
    a = line_dot2[1] - line_dot1[1]
    b = line_dot1[0] - line_dot2[0]
    c = (line_dot2[0] * line_dot1[1]) - (line_dot1[0] * line_dot2[1])
    return (math.fabs(a * dot[0] + b * dot[1] + c)) / \
           (math.sqrt(pow(line_dot2[1] - line_dot1[1], 2) + pow(line_dot1[0] - line_dot2[0], 2)))


class GaugeReader:
    def __init__(self, rect, min_vec, max_vec, buffer_len):
        self.angle_buffer = [0] * buffer_len
        self.top = rect[0][1]
        self.bottom = rect[1][1]
        self.left = rect[0][0]
        self.right = rect[1][0]
        if self.left > self.right:
            self.left, self.right = self.right, self.left
        if self.top > self.bottom:
            self.top, self.bottom = self.bottom, self.top
        self.gauge_center_x = (min_vec[0][0] + max_vec[0][0]) / 2 - self.left
        self.gauge_center_y = (min_vec[0][1] + max_vec[0][1]) / 2 - self.top
        self.gauge_length = (length(min_vec[0][1], min_vec[1][1], min_vec[0][0], min_vec[1][0]) +
                             length(max_vec[0][1], max_vec[1][1], max_vec[0][0], max_vec[1][0])) / 2
        self.mask = np.empty(((self.bottom - self.top), (self.right - self.left)), dtype=np.uint8)
        self.mask_not = np.empty(((self.bottom - self.top), (self.right - self.left)), dtype=np.uint8)
        for i in range(self.mask.shape[0]):
            for j in range(self.mask.shape[1]):
                if length(self.gauge_center_x, j, self.gauge_center_y, i) < self.gauge_length:
                    self.mask[i][j] = 255
                    self.mask_not[i][j] = 0
                else:
                    self.mask[i][j] = 0
                    self.mask_not[i][j] = 255

        self._y1 = [np.array([[1, 1, 1], [0, 0, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 1, 1], [0, 0, 1], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 0, 1], [0, 0, 1], [0, 0, 1]], dtype=np.uint8),
                    np.array([[0, 0, 0], [0, 0, 1], [0, 1, 1]], dtype=np.uint8),
                    np.array([[0, 0, 0], [0, 0, 0], [1, 1, 1]], dtype=np.uint8),
                    np.array([[0, 0, 0], [1, 0, 0], [1, 1, 0]], dtype=np.uint8),
                    np.array([[1, 1, 0], [1, 0, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[1, 1, 0], [1, 0, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 0, 0], [0, 0, 1], [0, 1, 1]], dtype=np.uint8),
                    np.array([[1, 1, 0], [1, 0, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 1, 1], [0, 0, 1], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 0, 0], [1, 0, 0], [1, 1, 0]], dtype=np.uint8)]  # HMT模板矩阵

        self._y2 = [np.array([[0, 0, 0], [0, 1, 0], [1, 1, 1]], dtype=np.uint8),
                    np.array([[0, 0, 0], [1, 1, 0], [1, 1, 0]], dtype=np.uint8),
                    np.array([[1, 0, 0], [1, 1, 0], [1, 0, 0]], dtype=np.uint8),
                    np.array([[1, 1, 0], [1, 1, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[1, 1, 1], [0, 1, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 1, 1], [0, 1, 1], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 0, 1], [0, 1, 1], [0, 0, 1]], dtype=np.uint8),
                    np.array([[0, 0, 0], [0, 1, 1], [0, 1, 1]], dtype=np.uint8),
                    np.array([[0, 1, 1], [1, 1, 0], [0, 0, 0]], dtype=np.uint8),
                    np.array([[0, 0, 0], [0, 1, 1], [1, 1, 0]], dtype=np.uint8),
                    np.array([[0, 0, 0], [1, 1, 0], [0, 1, 1]], dtype=np.uint8),
                    np.array([[1, 1, 0], [0, 1, 1], [0, 0, 0]], dtype=np.uint8)]  # HMT模板矩阵

    def _pre_process(self, frame):
        frame = frame[self.top:self.bottom, self.left:self.right]
        frame_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        frame_gray = cv2.medianBlur(frame_gray, 3)
        threshold, frame_bin = cv2.threshold(frame_gray, 0, 255, cv2.THRESH_OTSU)  # 方法选择为THRESH_OTSU 类间方差最大
        frame_masked1 = cv2.bitwise_and(self.mask, frame_bin)
        frame_masked2 = cv2.bitwise_or(self.mask_not, frame_bin)
        if frame_masked1[frame_masked1 == 255].size > frame_masked2[frame_masked2 == 0].size:
            frame_bin = cv2.bitwise_not(frame_masked2)
        else:
            frame_bin = frame_masked1
        
        return frame_bin

    def _getlines(self, frame_bin):
        for i in range(ITERATION):
            erode1 = cv2.erode(cv2.bitwise_not(frame_bin), self._y1[i % 12])
            erode2 = cv2.erode(frame_bin, self._y2[i % 12])
            frame_bin = cv2.subtract(frame_bin, cv2.bitwise_and(erode1, erode2))
        frame_bin = cv2.dilate(frame_bin, np.ones([2, 2]))
        lines = cv2.HoughLinesP(frame_bin, 1, np.pi / 180.0, 20, minLineLength=20)
        return lines

    def _cal_angle(self, lines):
        vector_x = []
        vector_y = []
        q1 = 0.0
        q2 = 0.0
        q3 = 0.0
        q4 = 0.0
        for x1, y1, x2, y2 in lines[:, 0]:
            len1 = length(self.gauge_center_x, x1, self.gauge_center_y, y1)
            len2 = length(self.gauge_center_x, x2, self.gauge_center_y, y2)
            dist = dot2linedistance((self.gauge_center_x, self.gauge_center_y), (x1, y1), (x2, y2))
            if (len1 < self.gauge_length and len2 < self.gauge_length) and dist < self.gauge_length / 10:
                for dots_xy in (len1, x1, y1), (len2, x2, y2):
                    case = (dots_xy[1] - self.gauge_center_x > 0, dots_xy[2] - self.gauge_center_y > 0)
                    if case == (True, True):
                        q4 = q4 + dots_xy[0]
                    elif case == (True, False):
                        q1 = q1 + dots_xy[0]
                    elif case == (False, True):
                        q3 = q3 + dots_xy[0]
                    elif case == (False, False):
                        q2 = q2 + dots_xy[0]

                vector_x.append(x2 - x1)
                vector_y.append(y2 - y1)

        if len(vector_x):
            sum_x = np.sum(vector_x)
            sum_y = np.sum(vector_y)
            theta = math.asin(sum_y / math.sqrt(sum_x ** 2 + sum_y ** 2)) * 180 / np.pi
            if math.fabs(theta) < 45:
                if q1 + q4 > q2 + q3:
                    angle = theta + 180
                else:
                    angle = theta
            else:
                if q1 + q2 > q3 + q4:
                    if q1 > q2:
                        angle = 90 + math.asin(sum_x / math.sqrt(sum_x ** 2 + sum_y ** 2)) * 180 / np.pi
                    else:
                        angle = 90 - math.asin(sum_x / math.sqrt(sum_x ** 2 + sum_y ** 2)) * 180 / np.pi
                else:
                    if q3 > q4:
                        angle = -90 + math.asin(sum_x / math.sqrt(sum_x ** 2 + sum_y ** 2)) * 180 / np.pi
                    else:
                        angle = 270 - math.asin(sum_x / math.sqrt(sum_x ** 2 + sum_y ** 2)) * 180 / np.pi
            return angle
        return None

    def _filter(self, angle):
        if angle is not None:
            self.angle_buffer.append(angle)
            self.angle_buffer.pop(0)
            return np.median(self.angle_buffer)
        return None

    def find_angle(self, frame):
        frame_bin = self._pre_process(frame)
        lines = self._getlines(frame_bin)
        if lines is not None:
            angle = self._cal_angle(lines)
            angle = self._filter(angle)
            return angle
        else:
            return None


def main():
    img = np.zeros((512, 512, 3), np.uint8)
    cv2.namedWindow('image')
    cv2.setMouseCallback('image', draw_region)
    cap = cv2.VideoCapture(1)
    global cap_frame,angle_list
    while True:
        ret, cap_frame = cap.read()
        if ret:
            cv2.imshow('image', frame_buffer)
            k = cv2.waitKey(1) & 0xFF
            if k == ord('y'):
                break
            elif k == 27:
                break
    rect = region.copy()
    cv2.setMouseCallback('image', draw_vec)
    while True:
        ret, cap_frame = cap.read()
        if ret:
            cv2.imshow('image', frame_buffer)
            k = cv2.waitKey(1) & 0xFF
            if k == ord('y'):
                break
            elif k == 27:
                break
    min_vec = region.copy()
    while True:
        ret, cap_frame = cap.read()
        if ret:
            cv2.imshow('image', frame_buffer)
            k = cv2.waitKey(1) & 0xFF
            if k == ord('y'):
                break
            elif k == 27:
                break
    cv2.destroyAllWindows()
    max_vec = region.copy()
    gr = GaugeReader(rect, min_vec, max_vec, 15)
    print("rect:", rect)
    while True:
        ret, frame = cap.read()
        if ret:
            angle = gr.find_angle(frame)
            if angle is not None:
                angle_list.append(angle)
                cv2.line(frame, (int(gr.left + gr.gauge_center_x), int(gr.top + gr.gauge_center_y)),
                         (int(gr.left + gr.gauge_center_x - gr.gauge_length * math.cos(angle * np.pi / 180)),
                          int(gr.top + gr.gauge_center_y - gr.gauge_length * math.sin(angle * np.pi / 180))), (0, 255, 0), 3)
                cv2.putText(frame, str(angle), (0, 20), cv2.FONT_HERSHEY_PLAIN, 1, (0, 0, 255), 2)
                cv2.imshow("frame", frame)
                k = cv2.waitKey(1) & 0xFF
                if k == ord('y'):
                    break
                elif k == 27:
                    break
    angled_list = np.array(angle_list)
    plt.title("vec")
    plt.plot(np.array(angled_list))
    plt.show()


if __name__ == '__main__':
    # main()
    frame = cv2.imread("yibiao.png")
    rect = ((0,0),(456,452))
    min_vec = ((236,231),(320,101))
    max_vec = ((236,231),(323,315))
    gr = GaugeReader(rect, min_vec, max_vec, 1)
    angle = gr.find_angle(frame)
    if angle is not None:
        angle_list.append(angle)
        cv2.line(frame, (int(gr.left + gr.gauge_center_x), int(gr.top + gr.gauge_center_y)),
                 (int(gr.left + gr.gauge_center_x - gr.gauge_length * math.cos(angle * np.pi / 180)),
                  int(gr.top + gr.gauge_center_y - gr.gauge_length * math.sin(angle * np.pi / 180))), (0, 255, 0),
                 3)
        cv2.putText(frame, str(angle), (0, 20), cv2.FONT_HERSHEY_PLAIN, 1, (0, 0, 255), 2)
        
        cv2.waitKey(0)