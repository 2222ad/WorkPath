import pandas as pd
from matplotlib import pyplot as plt
import numpy as np
import os
import sys

class Thompson(object):
    def __init__(self,rocker_num:int ,round:int ,reward_list: dict,expect:float) :
        """
        rocker_num:摇杆数量 \
        round :回合数 \ 
        reward_list :奖励列表 \
        expect :每个摇臂的初始期望 
        """
        self.rocker_num=rocker_num
        self.round=round
        self.reward_list=reward_list
        self.expect=expect
     
        #汤姆森采样的beta分布参数
        self.beta_a=dict(zip(range(1,rocker_num+1),rocker_num*[0]))
        self.beta_b=dict(zip(range(1,rocker_num+1),rocker_num*[0]))

        #字典记录每个摇臂的选择的次数
        self.rocker_count=dict(zip(range(1,rocker_num+1),rocker_num*[1e-6]))
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

    #选择摇臂
    def __choose_rocker(self):
        Beta_value={}
        for i in self.beta_a.keys():
            Beta_value[i]=np.random.beta(self.beta_a[i]+1,self.beta_b[i]+1)
        choose_rocker = max(Beta_value,key=lambda k:Beta_value[k])
        return choose_rocker

    # 强化训练
    def train(self):
        # 选择均值最大的摇臂
        for i in range(1,self.round+1):
            self.present_round=i
            choose_rocker=self.__choose_rocker()
            present_reward=self.get_reward(choose_rocker,i)
            self.cumulative_rewards_history[i]=self.cumulative_rewards_history[i-1]+present_reward

            if present_reward>=self.expect :
                self.beta_a[choose_rocker]=self.beta_a[choose_rocker]+1
            else :
                self.beta_b[choose_rocker]=self.beta_b[choose_rocker]+1

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
    
    k=Thompson(4,l,a,7)
    k.train()
    k.plot(colors='r',policy='Thompson',linestyle='-')
    # print(a[1][0])





