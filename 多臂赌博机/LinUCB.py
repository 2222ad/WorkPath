import pandas as pd
from matplotlib import pyplot as plt
import numpy as np
import os
import sys

class LinUCB:
    def __init__(self,rocker_num:int ,round:int  ,dimen :int ,
                 feature :list ,
                 reward_list: dict,
                 alpha:float =0.2):
        ''''
        rocker_num:摇杆数量 \
        round :回合数 \ 
        reward_list :奖励列表 \
        feature :特征为Round * rocker_num * dimen 的list 或者array
        dimen ：特征维度
        alpha: 探索几率
        '''
        self.rocker_num=rocker_num
        self.round=round
        self.dimen=dimen
        self.reward_list=reward_list
        self.feature=feature
        self.alpha=alpha

        self.A=np.array([np.identity(dimen) for x in range(rocker_num+1)])
        self.b=np.array([np.zeros((dimen,1)) for x in range(rocker_num+1)])

        #每个摇杆的概率分布
        self.p=dict(zip(range(1,rocker_num+1),rocker_num*[0]))

        #字典记录每个摇臂的选择的次数
        self.rocker_count=dict(zip(range(1,rocker_num+1),rocker_num*[0]))
        #字典记录每个摇臂的均值
        self.rocker_mean=dict(zip(range(1,rocker_num+1),rocker_num*[0]))

        #列表记录每个行动选择的摇臂（1,rocker_num）
        self.action=[0]*(round+1)
        #当前回合
        self.present_round=0
        #每个回合的累计回报
        self.cumulative_rewards_history=[0]*(round+1)
    
    # 返回对应摇杆的第Round轮的奖励
    def get_reward(self ,Rocker:int ,Round:int)-> int:
        return self.reward_list[Rocker][Round-1]
    
    #绘制图像
    def plot(self,colors,policy,linestyle):
        plt.figure(1)
        plt.plot(range(1,self.round+1),
                 self.cumulative_rewards_history[1:self.round+1],
                 colors,label=policy,linestyle=linestyle)
        plt.legend()
        plt.xlabel('n',fontsize=18)
        plt.ylabel('total rewards', fontsize=18)
        plt.show()

    # 强化训练
    def train(self):
        # 选择均值最大的摇臂
        for i in range(1,self.round+1):
            self.present_round=i

            for j in range(1,self.rocker_num+1):
                A_inv=np.linalg.inv(self.A[j])
                theta_hat=np.dot(A_inv,self.b[j])
                self.p[j]=np.dot(self.feature[i][j],theta_hat)+ self.alpha * \
                            np.sqrt(np.dot(np.dot(self.feature[i][j],A_inv), \
                                           self.feature[i][j].reshape(-1,1)))[0]

            # A 与 b的迭代
            choose_rocker=max(self.p,key=lambda k:self.p[k])
            present_reward=self.get_reward(choose_rocker,i)

            self.A[choose_rocker]=self.A[choose_rocker]+ \
                np.dot(self.feature[i][choose_rocker].reshape(-1,1),self.feature[i][choose_rocker])
            self.b[choose_rocker]=self.b[choose_rocker]+present_reward * self.feature[i][choose_rocker].reshape(-1,1)

            self.cumulative_rewards_history[i]=self.cumulative_rewards_history[i-1]+present_reward
            # 修改类变量
            self.action[i]=choose_rocker
            self.rocker_mean[choose_rocker]=(self.rocker_mean.get(choose_rocker,0)*self.rocker_count.get(choose_rocker,0) + present_reward) \
                                            /(self.rocker_count.get(choose_rocker,0)+1)
            
            self.rocker_count[choose_rocker]=self.rocker_count.get(choose_rocker,0)+1
            


if __name__=='__main__':
    l=1000
    a={}
    a[1]=np.random.normal(loc=4.0,scale=3,size=(l))
    a[2]=np.random.normal(loc=6.0,scale=2,size=(l))
    a[3]=np.random.normal(loc=4.5,scale=3.5,size=(l))
    a[4]=np.random.normal(loc=8,scale=7,size=(l))
    
    k=LinUCB(4,l,a,0.1,5)
    k.train()
    k.plot(colors='r',policy='UCB',linestyle='-')
    # print(a[1][0])










