# 使用 Python 实现Lasso回归算法（坐标下降法）

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn import datasets
from sklearn import linear_model
from sklearn.model_selection import train_test_split

# 读取数据

def load_data():
    boston = datasets.load_boston()
    X = boston.data
    y = boston.target
    return X, y

# 数据预处理

def data_preprocess(X, y):
    # 划分训练集和测试集
    X_train, X_test, y_train, y_test = train_test_split(X, y, random_state=33)
    # 数据标准化
    from sklearn.preprocessing import StandardScaler
    ss_X = StandardScaler()
    ss_y = StandardScaler()
    X_train = ss_X.fit_transform(X_train)
    X_test = ss_X.transform(X_test)
    y_train = ss_y.fit_transform(y_train.reshape(-1, 1))
    y_test = ss_y.transform(y_test.reshape(-1, 1))
    return X_train, X_test, y_train, y_test

# Lasso回归算法

def Lasso(X_train, X_test, y_train, y_test, alpha=0.01, max_iter=10000):
    # 初始化参数
    m, n = X_train.shape
    theta = np.zeros((n, 1))
    # 初始化损失函数值
    J = []
    # 初始化残差
    r = y_train - X_train.dot(theta)
    # 初始化梯度
    gradient = np.zeros((n, 1))
    # 初始化最大迭代次数
    k = 0
    # 迭代
    while k < max_iter:
        k += 1
        # 更新theta
        for j in range(n):
            # 计算梯度
            gradient[j] = - X_train[:, j].T.dot(r) / m + alpha * np.sign(theta[j])
            # 更新theta
            theta[j] = theta[j] - alpha * gradient[j]
        # 更新残差
        r = y_train - X_train.dot(theta)
        # 更新损失函数值
        J.append(1 / (2 * m) * r.T.dot(r) + alpha * np.sum(np.abs(theta)))
        # 判断是否收敛
        if np.linalg.norm(gradient) < 1e-5:
            break
    # 计算测试集的损失函数值
    J_test = 1 / (2 * len(y_test)) * np.sum((y_test - X_test.dot(theta)) ** 2) + alpha * np.sum(np.abs(theta))
    return theta, J, J_test, k

# 调用函数

if __name__ == '__main__':

    # 读取数据
    X, y = load_data()
    # 数据预处理
    X_train, X_test, y_train, y_test = data_preprocess(X, y)
    # 调用Lasso回归算法
    theta, J, J_test, k = Lasso(X_train, X_test, y_train, y_test)
    # 输出结果
    print('参数theta：', theta)
    print('损失函数值：', J[-1])
    print('测试集损失函数值：', J_test)
    print('迭代次数：', k)
    # 画出损失函数值的变化曲线
    plt.plot(J)
    plt.xlabel('Iteration')
    plt.ylabel('Loss')
    plt.show()
    # 调用sklearn中的Lasso回归算法
    lasso = linear_model.Lasso(alpha=0.01, max_iter=10000)
    lasso.fit(X_train, y_train)
    print('参数theta：', lasso.coef_)
    print('截距：', lasso.intercept_)
    print('测试集损失函数值：', np.mean((lasso.predict(X_test) - y_test) ** 2))
    print('迭代次数：', lasso.n_iter_)
    # 画出损失函数值的变化曲线
    plt.plot(lasso.coef_)
    plt.xlabel('Iteration')
    plt.ylabel('Loss')
    plt.show()