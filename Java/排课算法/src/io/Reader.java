package io;


import inputentity.Classes;
import inputentity.Curriculum;
import inputentity.Teacher;

import java.util.*;

public class Reader {
    public static List<Curriculum> getCurInput(String path, int index) throws Exception {
        ArrayList<Curriculum> list = new ArrayList<>();
        List<Map<String, String>> maps = ExcelDataUtil.redExcel(path + "\\C0_" + index + ".xlsx");
        for (Map<String, String> map : maps) {
            Map<String, String> constraintMap = new HashMap<>();
            String[] constraints = map.get("Soft constraints").split(";");

            for (String s : constraints) {
                if (s == null || s.length() <= 0) {
                    continue;
                }
                String[] ms = s.split(":");
                constraintMap.put(ms[0], ms[1]);
            }

            Curriculum curriculum = new Curriculum(map.get("Course number"),
                    Arrays.asList(map.get("Class").split(",")),
                    Arrays.asList(map.get("Teacher").split(",")),
                    map.get("Total credits hours"), constraintMap,Integer.parseInt(map.get("Type of classroom")));
//            if (map.size() > 5) {
//                curriculum.setConstraint(Arrays.asList(map.get("软约束").split(";")));
//            }
            list.add(curriculum);
        }
        return list;
    }

    public static List<Teacher> getTeaInput(String path, int index,int weekNum) throws Exception {
        ArrayList<Teacher> list = new ArrayList<>();
        List<Map<String, String>> maps = ExcelDataUtil.redExcel(path + "\\K0_" + index + ".xlsx");
        for (Map<String, String> map : maps) {
            boolean[][] isForbidden = getForbidden(weekNum,map, true);
            Teacher teacher = new Teacher(map.get("Teacher number"), isForbidden);
            list.add(teacher);
        }
        return list;
    }

    public static List<Classes> getClassInput(String path, int index) throws Exception {
        ArrayList<Classes> list = new ArrayList<>();
        List<Map<String, String>> maps = ExcelDataUtil.redExcel(path + "\\T0_" + index + ".xlsx");
        for (int i = 0; i < maps.size(); i++) {
            Classes classes = new Classes(maps.get(i).get("Class number"), maps.get(i).get("Class name"), null);
            //尽管说T2-T5是一样的,T6-T10是一样的，但还是读吧，更具普遍性，后期有需要再改
            List<Map<String, String>> classT = ExcelDataUtil.redExcel(path + "\\T" + (i + 1) + "_" + index + ".xlsx");
            List<Set<Integer>> timeList = new ArrayList<>();
            for (int j = 0; j < 25; j++) {
                Map<String, String> map = classT.get(j);
                timeList.add(getKey(map));   //得到不可安排课程的周次
            }
            classes.setIsForbidden(timeList);
            list.add(classes);
        }
        return list;

    }
    public static Map<String,Integer> getClassroomInput(String path, int index) throws Exception {
        Map<String,Integer> list = new HashMap<>();
        List<Map<String, String>> maps = ExcelDataUtil.redExcel(path + "\\D_" + index + ".xlsx");
        for (Map<String, String> map : maps) {
            list.put(map.get("Type of classroom"),Integer.parseInt(map.get("amount")));
        }
        return list;
    }

    private static Set<Integer> getKey(Map<String, String> map) {
        // 使用for循环遍历
        Set<Integer> set = new HashSet<>();
        boolean flag = true;
        for (Map.Entry<String, String> m : map.entrySet()) {
            if (flag) {
                flag = false;
                continue;
            }
            String key;
//            System.out.println(m.getValue().getClass().getName());
            if (!m.getValue().equals("0")) {
                key = m.getKey();
                set.add((int) Double.parseDouble(key));
            }
        }
        return set;
    }

    private static boolean[][] getForbidden(int weekNum,Map<String, String> map, boolean book) {
        boolean[][] isForbidden = new boolean[weekNum][25];
        // 使用for循环遍历
        boolean flag1 = true, flag2 = true;
        for (Map.Entry<String, String> m : map.entrySet()) {
            if (flag1) {
                flag1 = false;
                continue;
            }
            if (flag2) {
                flag2 = false;
                continue;
            }
            if (!m.getValue().equals("0")) {
                int key = (int) Double.parseDouble(m.getKey());
                String[] values = m.getValue().split(",");
                if (book) {
                    for (String value : values) {
                        int valueInt = Integer.parseInt(value);
                        isForbidden[valueInt - 1][key - 1] = true;
                    }
                } else if (!book) {
                    for (String value : values) {
                        int valueInt = Integer.parseInt(value);
                        isForbidden[key - 1][valueInt - 1] = true;
                    }
                }
            }
        }
        return isForbidden;

    }

}
