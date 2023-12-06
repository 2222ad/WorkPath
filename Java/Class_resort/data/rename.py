import os
import pandas as pd


def re_name(path):
    for file in os.listdir(path):
        file_path = os.path.join(path, file)
        # 判断这个文件是否是文件夹,是文件夹的话就调用自己,把路径拼接好传过去
        if os.path.isdir(file_path):
            re_name(file_path)
        else:  # 如果不是文件夹,就开始改名字
            if ("xls" in file_path and "xlsx" not in file_path):
                file_new = file.replace("xls", "xlsx")
                file_new_path = os.path.join(path, file_new)
                # print(file_new_path)
                print(file_new_path)
				# pd.read_excel(file_path).to_excel(file_new_path, index=False)
                if ("K" in file_path):
                   data = pd.read_excel(file_path)
                   data.columns = col
                   # data.drop(0, inplace=True)
                   # data.fillna(0, inplace=True)
                   data.to_excel(file_new_path, index=False)
                else:
                   pd.read_excel(file_path).to_excel(file_new_path, index=False)


col = [
    'Teacher Number', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11',
    '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23',
    '24', '25'
]

re_name(os.getcwd())
i = input()