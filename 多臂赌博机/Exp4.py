import pandas as pd
from matplotlib import pyplot as plt
import numpy as np
import os
import sys

'''
***重要警告***
reward_list中的所有奖励都要归一化后再运行,否则可能会有始终选择一个摇杆的情况发生
另外相关文献有误，原文也是有错误，我不确定p和w的迭代规则
'''

class Exp4(object):
    def __init__(self,rocker_num:int ,round:int ,reward_list: dict,detla:float,
                 prompt_num:int,prompt:list,p_min:float) :
        """
        rocker_num:摇杆数量 \
        round :回合数 \ 
        reward_list :奖励列表 \
        detla :探索比例 >0
        prompt——num :专家模型数量
        prompt_num :专家模型，为一个二维数组 [prompt_num * rocker_num]
        """
        try :
            if p_min<0 or p_min>1/rocker_num :
                raise Exception("p_min should between [0,1/rocker_num]")
        except Exception as e:
            print(e)
        
        self.rocker_num=rocker_num
        self.round=round
        self.reward_list=reward_list
        self.detla=detla
        self.prompt_num=prompt_num
        self.prompt=prompt
        self.p_min=p_min
     
        #每个专家推荐的权重
        self.w=dict(zip(range(1,rocker_num+1),prompt_num*[1]))
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
    
    #概率p迭代
    def __p_iteration(self):
        w_sum=sum(self.w.values())
        for i in range(1,self.rocker_num+1):
            prompt_sum=0
            for j in range(self.prompt_num):
                prompt_sum=prompt_sum+self.w[j]*self.prompt[j][i-1]

            self.p[i]=(1-self.p_min*self.rocker_num)*prompt_sum/w_sum+self.p_min
    
    #权重w迭代
    def __w_iteration(self,choose_rocker):
        fixed=np.sqrt(np.log(self.prompt_num/self.detla)/self.rocker_num/self.round)

        r_hat=self.get_reward(choose_rocker,self.present_round)/self.p[choose_rocker]

        for i in range(self.prompt_num):
            y_hat=self.prompt[i][choose_rocker-1]*r_hat

            v_hat=0
            for j in range(1,self.rocker_num+1):
                v_hat=v_hat+self.prompt[i][j-1]/self.p[j]
            
            self.w[i]=self.w[i]*np.exp(self.p_min/2*(y_hat+v_hat*fixed))

    #根据概率分布选择摇臂
    def __choose_rocker(self,random):
        sum=0
        for i in self.p.keys():
            sum=sum+self.p[i]
            if sum>=random :
                return i

    # 强化训练
    def train(self):
        # 选择均值最大的摇臂
        for i in range(1,self.round+1):
            self.present_round=i
            self.__p_iteration()
            choose_rocker=self.__choose_rocker(np.random.random())
            self.__w_iteration(choose_rocker)

            present_reward=self.get_reward(choose_rocker,i)
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
    a[4]=np.random.normal(loc=2,scale=7,size=(l))
    

    k=Exp4(4,l,a,0.1)
    k.train()
    k.plot(colors='r',policy='Exp3',linestyle='-')
    print(k.rocker_count)
    # print(a[1][0])
