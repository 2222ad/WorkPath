import pandas as pd
import numpy as np

def err_func(theta, X, y):
    m = len(y)
    temp = X.dot(theta) - y
    return 1/(2*m) * temp.T.dot(temp)

# 当前梯度
def gradient_func(theta, X, y):
    m = len(y)
    return 1/m * X.T.dot(X.dot(theta) - y)

# 梯度下降
def gradient_descent(X, y,  alpha, iters=10000):
    X=np.mat(X)
    y=np.mat(y).reshape(-1,1)
    theta = np.zeros((X.shape[1], 1))
    cost_list = [err_func(theta, X, y)]
    k=0
    while k < iters:
        k += 1
        gradient=gradient_func(theta, X, y)

        # 梯度下降法的终止条件
        if np.all(np.abs(gradient) < 1e-5):
            break
            
        theta = theta - alpha * gradient
        cost_list.append(err_func(theta, X, y))
        # print('第{}次迭代，损失函数值为：{}'.format(k, cost_list[-1]))
    print('第{}次迭代，损失函数值为：{}'.format(k, cost_list[-1]))
    print('theta:',theta)
    return theta, cost_list[-1],k

# 预测
def predict(X, theta):
    return X.dot(theta)