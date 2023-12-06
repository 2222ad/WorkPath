'''
This module implements the Softmax Regression algorithm for multi-class classification.

The Softmax_regression class provides methods for training the model and making predictions.

Example usage:
    model = Softmax_regression(max_iter=10000, learning_rate=0.0001, lambda_=0)
    model.fit(X, y)
    predictions = model.predict(X_test)

Author: [heyulin]
Date: [2023/12/6]
'''

import pandas as pd
import numpy as np
import math
import matplotlib.pyplot as plt
import requests
import os


class Softmax_regression:
    # 初始化
    def __init__(self, max_iter=10000, learning_rate=0.0001,lambda_=0):
        '''
        初始化SoftMax模型
        :param max_iter: 最大迭代次数
        :param learning_rate: 学习率(默认为0.00001,太大会造成不收敛 (๑ó﹏ò๑))
        :param lambda_: 正则化系数(默认为0,即不进行正则化)
        '''
        self._w = None
        self._one_hot_dict={}
        self._max_iter = max_iter
        self._learning_rate = learning_rate
        self._lambda_ = lambda_
        self._iter_count=0
        self._loss=[]
    
    # 训练
    def fit(self, X, y):
        X_mat=np.mat(X)
        y_mat=np.mat(y).T

        class_num = len(np.unique(y))
        feature_num = X.shape[1]
        sample_num = X.shape[0]

        # 初始化权重
        self._w = np.random.rand(feature_num,class_num)
        
        #one_hot_dict将标签与one_hot编码对应
        temp=0
        for i in np.unique(y):
            self._one_hot_dict[i]=temp
            temp+=1

        #得到y的one_hot编码
        y_onehot = self.one_hot(y,sample_num,class_num)
        
        # 梯度下降
        for i in range(self._max_iter):
            self._iter_count+=1
            y_hat = self.softmax(X_mat)
            # 计算梯度
            grad = -1.0 * X_mat.T * (y_onehot - y_hat) / sample_num + self._lambda_ * self._w
            # 判断是否收敛
            if np.linalg.norm(grad) < 1e-4:
                break
            
            # 更新权重
            self._w -= self._learning_rate * grad   
            # 计算损失
            if i % 100 == 0:
                loss = self.loss(X,y)
                self._loss.append(loss)
        
    # one_hot编码
    def one_hot(self,y,sample_num,class_num):
        y_onehot = np.zeros((sample_num,class_num))
        for i in range(sample_num):
            y_onehot[i,self._one_hot_dict[y[i]]]=1
        return y_onehot
        
    # 计算softmax
    # 重点哈，不写的减去最大值，会出现nan，debug搞了3个小时(ಥ_ಥ)
    def softmax(self,X_mat):
        y_hat = X_mat * self._w
        # 将y_hat每一行减去该行最大值
        y_hat = y_hat - np.max(y_hat,axis=1)
        y_hat = np.exp(y_hat)
        y_hat = y_hat / np.sum(y_hat,axis=1)
        return y_hat
    
    # 损失函数
    def loss(self,X,y):
        X_mat=np.mat(X)
        y_mat=np.mat(y).T
        sample_num = X.shape[0]
        class_num = len(np.unique(y))
        y_hat = self.softmax(X_mat)

        y_onehot = self.one_hot(y,sample_num,class_num)
        loss = -1.0 * np.sum(np.multiply(y_onehot,np.log(y_hat))) / sample_num 
        return loss
    
    # 
    def get_w(self):
        return self._w
    
    def get_iter_count(self):
        return self._iter_count



if __name__=="__main__":
    # 读取数据
    def download_data(url, filename):
        if not os.path.exists(filename):
            response = requests.get(url)
            with open(filename, 'wb') as f:
                f.write(response.content)
    # 读取数据
    url = 'http://archive.ics.uci.edu/ml/machine-learning-databases/wine-quality/winequality-white.csv'
    filename = 'winequality-white.csv'
    download_data(url, filename)
    wine = pd.read_csv(filename, sep=';')
    X=wine.iloc[:,:-1]
    y=wine.iloc[:,-1]

    model = Softmax_regression(max_iter=10000,learning_rate=0.00001)
    model.fit(X,y)

    plt.plot(model._loss)
    plt.xlabel('iter_count')
    plt.ylabel('loss')
    plt.show()

