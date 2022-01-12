# -*- coding: utf-8 -*-

import numpy as np
import cv2
import pandas as pd

faceCascade = cv2.CascadeClassifier('Cascades/haarcascade_frontalface_default.xml')
cap = cv2.VideoCapture(0)
cap.set(3,640)  # set Width
cap.set(4,480)  # set Height

import socket
import time

host = '192.168.137.102'  # ip
port = 9995  # 포트번호

server_sock = socket.socket(socket.AF_INET)
server_sock.bind((host, port))
server_sock.listen(1)
print('기다리는 중...')
out_data = int(1)

client_sock, addr = server_sock.accept()  # 연결승인
print("연결승인")

if client_sock:  # client_sock 가 null 값이 아니라면 (연결승인 되었다면)
    print('Connected by?!', addr) # 연결주소 print
    in_data = client_sock.recv(1024) # 안드로이드에서 "refresh" 전송
    print('rcv :', in_data.decode("utf-8")) #전송 받은 값 디코딩

while True:
    try:
        while in_data:  # 1초마다 안드로이드에 값 전달 (추후 , STOP , Connect 옵션 설정 가능)
            ret, img = cap.read()
            img = cv2.flip(img, -1)
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            faces = faceCascade.detectMultiScale(
                gray,
                scaleFactor=1.2,
                minNeighbors=5,
                minSize=(20, 20)
            )
            
            pandas = pd.DataFrame(faces)  # 넘파이 판다스로 변환
            print(pandas)
            
            for (x,y,w,h) in faces:
                    cv2.rectangle(img,(x,y),(x+w,y+h),(255,0,0),2)
                    roi_gray = gray[y:y+h, x:x+w]
                    roi_color = img[y:y+h, x:x+w]
                    
            cv2.imshow('video',img)
            k = cv2.waitKey(30) & 0xff
            if k == 27: # press 'ESC' to quit
                break
        
            if pandas.empty:  # 판다스가 비어있다면
                client_sock.send("safe".encode("utf-8"))
                print("safe")
            else:
                client_sock.send(str(out_data).encode("utf-8"))  # int 값을 string 으로 인코딩해서 전송, byte 로 전송하면 복잡함
                print('send :', out_data)
                out_data = out_data+1  # 전송값+1
                print("사람이 있을 경우")
            time.sleep(1)
        
    except Exception as e:
        print(e)
        print('disConnected by', addr)
        break

cap.release()
cv2.destroyAllWindows()

client_sock.close()
server_sock.close()
