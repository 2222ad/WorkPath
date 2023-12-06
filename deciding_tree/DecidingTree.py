'''
This module implements the DecisionTree class, which represents a decision tree classifier.
The DecisionTree class uses the C4.5 or CART algorithm to construct the decision tree.
It supports training, prediction, accuracy calculation, and visualization of the decision tree.

Example usage:
    model = DecisionTree(max_depth=5, min_samples_split=10, min_impurity=1e-3, mothod='CART', is_pruning=True)
    model.fit(X, y)
    predictions = model.predict(X_test)
    accuracy = model.accuracy(y_test, predictions)
    model.plot_tree()

Author: [heyulin]
Date: [2023/12/6]
'''

from typing import Any
from sklearn.model_selection import train_test_split
import pandas as pd
import numpy as np
import math
import matplotlib.pyplot as plt
import requests
import os
import copy

# 定义树节点值
class tree_node:
    def __init__(self,feature=None,threshold=None,left=None,right=None,value=None,parent=None):
        self.feature=feature
        self.threshold=threshold
        self.left=left
        self.right=right
        self.value=value
        self.parent=parent
    
    #判断是否为叶子节点
    def is_leaf(self):
        return self.value is not None
    
    def __setattribute__(self,name,value):
        self.__dict__[name]=value
    
    def __setitem__(self,key,value):
        self.__dict__[key]=value

    # Obeject类重载
    def __repr__(self):
        if self.is_leaf():
            return 'Leaf:{:.3f}'.format(self.value)
        else:
            return 'Node:{:.3f}'.format(self.threshold)
    
    # Obeject类重载__getitem__方法
    def __getitem__(self,key):
        return self.__dict__[key]

    def __getattr__(self, __name: str):
        return self.__dict__[__name]
    
    # get_parent
    def get_parent(self):
        return self.parent

# 定义决策树
class DecisionTree:
    # 初始化
    '''ArithmeticError
    :param max_depth: 最大深度(默认为5)
    :param min_samples_split: 最小切分样本数(默认为2)
    :param min_impurity: 最小信息增益(默认为1e-7)
    :mothod: C4.5 or CART
    :pruning: 是否剪枝
    '''
    def __init__(self,max_depth=5,min_samples_split=2,min_impurity=1e-7,
                    mothod='C4.5',is_pruning=False):
        self._root=None
        self._max_depth=max_depth
        self._min_samples_split=min_samples_split
        self._min_impurity=min_impurity
        self._is_pruning=is_pruning
    
        # 检测mothod是否合法
        if mothod not in ['C4.5','CART']:
            raise ValueError('mothod must be C4.5 or CART')
        self.mothod=mothod
    
    # 计算基尼系数
    def _gini(self,y):
        # 统计y中每个类别的个数
        classes=np.unique(y)
        # 计算基尼系数
        gini=0
        for i in classes:
            gini+=np.sum(y==i)/len(y)*(1-np.sum(y==i)/len(y))
        return gini
    
    # 计算信息熵
    def _entropy(self,y):
        # 统计y中每个类别的个数
        classes=np.unique(y)
        # 计算信息熵
        entropy=0
        for i in classes:
            entropy+=np.sum(y==i)/len(y)*np.log2(np.sum(y==i)/len(y))
        return -entropy
    
    # 计算信息增益
    def _info_gain(self,y,y1,y2):
        if self.mothod=='C4.5':
            # 计算y的信息熵
            entropy=self._entropy(y)
            # 计算y1的信息熵
            entropy1=self._entropy(y1)
            # 计算y2的信息熵
            entropy2=self._entropy(y2)
            # 计算信息增益
            info_gain=entropy-(len(y1)/len(y)*entropy1+len(y2)/len(y)*entropy2)
            return info_gain
        elif self.mothod=='CART':
            # 计算y的基尼系数
            gini=self._gini(y)
            # 计算y1的基尼系数
            gini1=self._gini(y1)
            # 计算y2的基尼系数
            gini2=self._gini(y2)
            # 计算信息增益
            info_gain=gini-(len(y1)/len(y)*gini1+len(y2)/len(y)*gini2)
            return info_gain
    
    # 计算最优切分点
    def _get_split_point(self,X : pd.DataFrame,y : pd.Series):
        # 初始化最优切分点
        best_info_gain=-1
        best_feature=None
        best_threshold=None
        # 遍历每个特征
        for feature in X.columns:
            # 获取该特征的所有取值
            feature_values=X[feature].unique()
            # 遍历该特征的每个取值
            for threshold in feature_values:
                # 根据该特征的该取值切分数据集
                y1=y[X[feature]<=threshold]
                y2=y[X[feature]>threshold]
                # 如果切分后的数据集小于最小切分样本数，则跳过该次循环
                if len(y1)<self._min_samples_split or len(y2)<self._min_samples_split:
                    continue
                # 计算信息增益
                info_gain=self._info_gain(y,y1,y2)
                # 如果信息增益大于最优信息增益，则更新最优信息增益、最优特征、最优切分点
                if info_gain>best_info_gain:
                    best_info_gain=info_gain
                    best_feature=feature
                    best_threshold=threshold
        return best_feature,best_threshold
    
    # 递归构建决策树
    def _build_tree(self,X:pd.DataFrame,y:pd.DataFrame,parent_node=None,depth=0):
        # 如果当前节点的深度大于最大深度，则将当前节点的y值的众数设为叶子节点的值
        if depth>self._max_depth:
            return tree_node(value=y.value_counts().index[0],parent=parent_node)

        # 如果当前节点的样本数小于最小切分样本数，则将当前节点的y值的众数设为叶子节点的值
        if len(y)<self._min_samples_split or len(np.unique(y))==1 :
            return tree_node(value=y.value_counts().index[0],parent=parent_node)
        
        # 如果当前X只有一个特征，则将当前节点的y值的众数设为叶子节点的值
        if X.columns.size==1:
            return tree_node(value=y.value_counts().index[0],parent=parent_node)
        
        # 计算最优切分点
        feature,threshold=self._get_split_point(X,y)
        # 如果最优切分点的信息增益小于最小信息增益，则将当前节点的y值的众数设为叶子节点的值
        if feature is None or threshold < self._min_impurity:
            return tree_node(value=y.value_counts().index[0],parent=parent_node)
        
        new_col = X.columns.drop(feature)
        X_left  = X[new_col][X[feature]<=threshold]
        X_right = X[new_col][X[feature]>threshold]

        # print("第{:d}层feature:".format(depth),feature)
        # print("第{:d}层threshold:".format(depth),threshold)

        Cur_node=tree_node(feature,threshold,parent=parent_node)
        # 递归构建左子树
        left=self._build_tree(X_left,y[X[feature]<=threshold],Cur_node,depth+1)
        # 递归构建右子树
        right=self._build_tree(X_right,y[X[feature]>threshold],Cur_node,depth+1)
        # 将左子树和右子树设为当前节点的子节点
        Cur_node.left=left
        Cur_node.right=right
        # 返回当前节点
        return Cur_node
    
    # 训练
    def fit(self,X : pd.DataFrame,y : pd.DataFrame,X_test : pd.DataFrame =None ,y_test : pd.DataFrame =None):
        '''
        Train the decision tree classifier.

        :param X: The input features.
        :type X: pd.DataFrame
        :param y: The target values.
        :type y: pd.DataFrame
        :param X_test: The test features (optional).
        :type X_test: pd.DataFrame
        :param y_test: The test target values (optional).
        :type y_test: pd.DataFrame
        '''
        self._root=self._build_tree(X,y)
        self._node_merge(self._root)
        if X_test is not None and y_test is not None and self._is_pruning == True:
            self.pruning(X_test,y_test)


    # 预测,并且返回每个样本的叶子节点
    def predict(self,X,node=None):
        '''
        Predict the target values for the input features.

        :param X: The input features.
        :type X: pd.DataFrame
        :param node: The starting node for prediction (optional).
        :type node: tree_node
        :return: The predicted target values and the corresponding leaf nodes.
        :rtype: np.array, list
        '''
        # 预测结果列表
        y_leaf_node=[]
        y_pred=[]
        # 遍历每个样本
        for i in range(len(X)):
            # 从根节点开始
            sample=X.iloc[i,:]
            if node is None:
                node=self._root
            # 如果当前节点不是叶子节点，则遍历子树
            while not node.is_leaf():
                # 如果样本的特征值小于当前节点的切分点，则遍历左子树
                if sample[node['feature']]<=node['threshold']:
                    node=node['left']
                # 如果样本的特征值大于当前节点的切分点，则遍历右子树
                else:
                    node=node['right']
            # 将当前节点的值作为预测结果
            y_pred.append(node['value'])
            y_leaf_node.append(node)
        return np.array(y_pred),y_leaf_node
    
    # 计算准确率
    def accuracy(self,y,y_pred):
        '''
        Calculate the accuracy of the predicted target values.

        :param y: The true target values.
        :type y: np.array
        :param y_pred: The predicted target values.
        :type y_pred: np.array
        :return: The accuracy.
        :rtype: float
        '''
        return np.sum(y==y_pred)/len(y)
    
    # 可视化决策树
    def plot_tree(self):
        '''
        Visualize the decision tree.
        '''
        # 定义画布
        plt.figure(figsize=(15,8))
        # 定义子图
        ax=plt.subplot(111)
        # 隐藏坐标轴
        plt.axis('off')
        # 递归绘制决策树
        self._plot_tree(ax,self._root)
        # 显示图像
        plt.show()

    # 递归绘制决策树
    def _plot_tree(self,ax,node,x=0,y=0,level=3.5):
        # 如果当前节点是叶子节点，则绘制叶子节点
        if node.is_leaf():
            ax.text(x,y,node['value'],ha='center',va='center',fontsize=8,bbox=dict(facecolor='yellow',edgecolor='black',boxstyle='circle'))
        # 如果当前节点不是叶子节点，则绘制切分点
        else:
            ax.text(x,y,node['feature']+'\n'+str(node['threshold']),ha='center',va='center',fontsize=8,bbox=dict(facecolor='white',edgecolor='black',boxstyle='circle'))
            
            # 绘制连线
            ax.plot([x,x-level],[y,y-1],c='black')
            ax.plot([x,x+level],[y,y-1],c='black')

            # 绘制左子树
            self._plot_tree(ax,node['left'],x-level,y-1,level/2)
            # 绘制右子树
            self._plot_tree(ax,node['right'],x+level,y-1,level/2)

    # 获取node节点的所有叶子节点
    def _get_leaves(self,node):
        '''
        Get all the leaf nodes under the given node.

        :param node: The starting node.
        :type node: tree_node
        :return: The list of leaf nodes.
        :rtype: list
        '''
        # 如果当前节点是叶子节点，则将当前节点加入叶子节点列表
        if node.is_leaf():
            return [node]
        # 如果当前节点不是叶子节点，则遍历子树
        else:
            return self._get_leaves(node['left'])+self._get_leaves(node['right'])

    # 根据验证集数据对已经构建好的决策树子树进行剪枝
    def _data_merching(self,X_val,y_val):        
        # 存储每个叶子节点对应的数据y与y_pred
        y_leaf_value={}

        # 遍历每个样本
        for i in range(len(X_val)):
            # 从根节点开始
            sample=X_val.iloc[i,:]
            node=self._root
            # 如果当前节点不是叶子节点，则遍历子树
            while not node.is_leaf():
                # 如果样本的特征值小于当前节点的切分点，则遍历左子树
                if sample[node['feature']]<=node['threshold']:
                    node=node['left']
                # 如果样本的特征值大于当前节点的切分点，则遍历右子树
                else:
                    node=node['right']
            
            if type(y_leaf_value.get(node,0))==int:
                y_leaf_value[node]=[[i,y_val.iloc[i],node['value']]]
            else :
                y_leaf_value[node].append([i,y_val.iloc[i],node['value']])
        return y_leaf_value
    
    def _dfs_pruning(self,node,y_leaf_value):
        """
        warning: 此处不可使用elif，因为如果使用elif，当node['left']为叶子节点时，node['right']不会被遍历
        """
        if not node['left'].is_leaf():
            self._dfs_pruning(node['left'],y_leaf_value)

        if not node['right'].is_leaf():
            self._dfs_pruning(node['right'],y_leaf_value)
        
        if node['left'].is_leaf() and node['right'].is_leaf() :
            left=node['left']
            right=node['right']
            if left not in y_leaf_value.keys() or right not in y_leaf_value.keys():
                return
            y_index_val_pre=np.array(y_leaf_value[left]+y_leaf_value[right])
            y_index=y_index_val_pre[:,0]

            # 剪枝前准确度
            acc_not_prun=self.accuracy(y_index_val_pre[:,1],y_index_val_pre[:,2])
            # 剪枝后准确度
            acc_pruned=self.accuracy(y_index_val_pre[:,1],np.argmax(np.bincount(y_index_val_pre[:,1])))

            #删除子树
            if acc_pruned<acc_not_prun:
                node.value=np.argmax(np.bincount(y_index_val_pre[:,1]))
                node.left=None
                node.right=None
                node.feature=None
                node.threshold=None
                del y_leaf_value[left]
                del y_leaf_value[right]
                y_leaf_value[node]=y_index_val_pre.tolist()
            
    # 如果发现同样的子节点对应的两个叶子节点的值相同，则将这两个叶子节点合并
    def _node_merge(self,node):
        if not node['left'].is_leaf():
            self._node_merge(node['left'])
        if not node['right'].is_leaf():
            self._node_merge(node['right'])
        if node['left'].is_leaf() and node['right'].is_leaf():
            if node['left']['value']==node['right']['value']:
                node['value']=node['left']['value']
                node['left']=None
                node['right']=None
                node['feature']=None
                node['threshold']=None


    # 剪枝
    def pruning(self,X_test,y_test):
        y_leaf_value=self._data_merching(X_test,y_test)
        # print('数据匹配完毕')
        self._dfs_pruning(self._root,y_leaf_value)
        self._node_merge(self._root)

if __name__=="__main__":

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

    #随机取部分X,y的数据作为验证集
    X_train,X_test,y_train,y_test=train_test_split(X,y,test_size=0.05,random_state=42)
    model_CART=DecisionTree(max_depth=5,min_samples_split=10,min_impurity=1e-3,mothod='CART',is_pruning=True)
    model_CART.fit(X,y,X_test,y_test)
    model_CART.plot_tree()

        