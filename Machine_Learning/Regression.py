# 使用 Python 实现Ridge回归算法
import pandas as pd
import numpy as np

# MES计算
def err_func(theta, X, y):
    m = len(y)
    temp = X.dot(theta) - y
    return 1/(2*m) * temp.T.dot(temp)

# 当前梯度
def gradient_func(theta, X, y):
    m = len(y)
    return 1/m * X.T.dot(X.dot(theta) - y)

# 梯度下降
def gradient_descent(X, y,  alpha, theta=0,iters=10000):
    X=np.mat(X)
    y=np.mat(y).reshape(-1,1)
    if type(theta)==int:
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
    # print('第{}次迭代，损失函数值为：{}'.format(k, cost_list[-1]))
    # print('theta:',theta)
    return theta, cost_list[-1],k


# Ridge回归算法
def Ridge(X_train, y_train, Lambda=0.2):
    # 初始化参数
    X = np.mat(X_train)
    y = np.mat(y_train)

    XTX = X.T*X
    m, _ = XTX.shape
    I = np.matrix(np.eye(m))
    theta = (XTX + Lambda*I).I*X.T*y.T
    return theta,err_func(theta, X, y)

# Lasso回归算法
def Lasso_regression(X_train,y_train,Lambda=0.2,threshold=0.001,iters=50000):
    '''
    岭回归参数Lambda
    步长alpha
    '''
    X=np.mat(X_train)
    y=np.mat(y_train).reshape(-1,1)

    theta = np.zeros((X.shape[1], 1))
    
    err=err_func(theta, X, y)

    iter=0
    while iter < iters:
        iter += 1
        
        temp_theta=theta.copy()
        for k in range(X.shape[1]):
            # 计算z_k,p_k
            z_k = (X[:, k].T*X[:, k])[0, 0]
            p_k = 0
            for i in range(X.shape[0]):
                p_k += X[i, k]*(y[i, 0] - sum([X[i, j]*theta[j, 0] for j in range(X.shape[1]) if j != k]))
            if p_k < -Lambda/2:
                theta_k = (p_k + Lambda/2)/z_k
            elif p_k > Lambda/2:
                theta_k = (p_k - Lambda/2)/z_k
            else:
                theta_k = 0
            theta[k, 0] = theta_k

        # err_new = err_func(theta, X, y)
        # if abs(err_new - err) < threshold:
        #     break
        # err = err_new
        if (np.abs(theta-temp_theta)<threshold).all():
            break
        # print('第{}次迭代，损失函数值为：{}'.format(iter, err))

    return theta, err, iter


#局部加权线性回归
def LW_linear_Regression(testPoint,X_train,y_train,k=1.0):
    '''
    X_train:训练样本特征
    y_train:训练样本标签
    x_test:测试样本特征
    k:控制核的宽度参数
    '''
    X=np.mat(X_train)
    y=np.mat(y_train).reshape(-1,1)
    testPoint=np.mat(testPoint).reshape(1,-1)

    m=X_train.shape[0]
    n=X_train.shape[1]
    #计算权重矩阵
    W = np.mat(np.eye(m))
    for i in range(m):
        diffMat = testPoint - X[i,:]   #difference matrix
        W[i,i] = np.exp(diffMat*diffMat.T/(-2.0*k**2))

    #计算参数
    xTx=X.T*(W*X)
    if np.linalg.det(xTx)==0:
        # print('矩阵为奇异矩阵，不能求逆')
        return 0
    theta=xTx.I*(X.T*(W*y))
    return testPoint*theta