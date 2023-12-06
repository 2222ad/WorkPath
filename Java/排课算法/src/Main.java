import gcp.curclass.Cur;
import gcp.graphcoloring.TwoGraphPhrase;
import inputentity.Curriculum;
import inputentity.RawInput;
import io.Reader;
import io.Write;
import tabu.CurSet;
import tabu.TabuSearchSolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        for(int ite = 1 ; ite <= 1; ite+=1) {//TODO

            writeResult("第" + ite + "个算例...\n");

//            for(float pc = 0.1f; pc <= 0.9f ; pc+=0.2f) {
//                writeResult("变异" + pc + "...\n");

                int time = 1;//TODO
                for (int count = 0; count < time; count++) {//每个算例运行time次
                    int[] violate = new int[5]; //记录6个N_i的数量
                    ArrayList<Object> resultList = new ArrayList<>();

                    String inputPath = "D:\\排课\\data" + "/Instance" + ite;

                    Instant start = Instant.now();    // 当前时间点
//                int index = 1;    //算例编号1-8
                    int weekNum = 18;
//                String inputPath = args[0] +
//                String outputPath = args[1];

                    File file = new File(inputPath);
                    String[] fileList = file.list();
                    System.out.println(Arrays.toString(fileList));

                    // 读取数据
                    RawInput rawInput = new RawInput();
                    assert fileList != null;
                    for (String filename : fileList) {
                        if (filename.contains("C")) {
                            rawInput.setCurriculumList(Reader.getCurInput(inputPath + "/" + filename, ite));
                        }
                        if (filename.contains("K")) {
                            rawInput.setTeacherList(Reader.getTeaInput(inputPath + "/" + filename, ite, weekNum));
                        }
                        if (filename.contains("T")) {
                            rawInput.setClassList(Reader.getClassInput(inputPath + "/" + filename, ite));
                        }
                        if (filename.contains("D")) {
                            rawInput.setClassroomMap(Reader.getClassroomInput(inputPath + "/" + filename, ite));
                        }
                    }

                    // 两阶段图着色
                    List<List<Set<Cur>>> curSetsList = TwoGraphPhrase.twoGraphColoring(rawInput , 0.1f , 0.1f);
                    System.out.println(" 目前有" + curSetsList.size() + "种方案");

                    // 记录班级通识课时间占用以及约束2-教师无法排课时间段
                    // forbidden数组中true代表不可排，false说明该curSet在该时间段对应所有老师及课堂空闲可排
                    List<List<CurSet>> curSetsForList = getListForbidden(weekNum, rawInput, curSetsList);

                    double lastCost = Double.MAX_VALUE;
                    String result = null;
//                    System.out.print(" " + );
                    for (List<CurSet> curSetsFor : curSetsForList) {
                        //每一种方案
                        TabuSearchSolver tabuSearchSolver = new TabuSearchSolver(weekNum, 8, 30, 1, 1, 1,  2, 2, 1.0, 5.0, curSetsFor, rawInput.getCurriculumList());
                        tabuSearchSolver.solve();
                        //TODO
                        CurSet[][] schedule = tabuSearchSolver.getSchedule();
                        int[][][] schedule2 = new int[weekNum][][];
                        for (int i = 0; i < schedule.length; i++) {
//                            System.out.println(" \n第" + (i + 1) + "周：");
                            int len = 0;
                            for (CurSet Day : schedule[i]) {
                                if (Day.getCurSet() != null) {
                                    len = Math.max(len, Day.getCurSet().size());
                                }
                            }
                            if (len != 0) {
                                int[][] Week = new int[len][25];
                                for (int j = 0; j < 25; j++) {
                                    if (schedule[i][j].getCurSet() != null) {
                                        List<Cur> Day = new ArrayList<>(schedule[i][j].getCurSet());
                                        for (int k = 0; k < Day.size(); k++) {
                                            Week[k][j] = Day.get(k).getSupId() + 1;
                                        }
                                    }
                                }
                                schedule2[i] = Week;
                                for (int[] ints : Week) {
                                    for (int anInt : ints) {
//                                        System.out.printf("%7d", anInt);
                                    }
//                                    System.out.println();
                                }
                            }
                        }

                        io.Write write = new Write(ite + "\\" + count);
                        write.writeAll(schedule2);
                        for (int k = 0; k < rawInput.getClassList().size(); k++) {
                            int[][] schedule3 = new int[18][25];
                            for (int i = 0; i < schedule.length; i++) {
                                for (int j = 0; j < schedule[i].length; j++) {
                                    if (schedule[i][j].getCurSet() != null) {
                                        for (Cur c3 : schedule[i][j].getCurSet()) {
                                            if (rawInput.getCurriculumList().get(c3.getSupId()).getClassId().contains(Integer.toString(k + 1))) {
                                                schedule3[i][j] = c3.getSupId() + 1;
                                            }
                                        }
                                    }
                                }
                            }
                            write.writeClass(k, schedule3);
                        }


//                    if (tabuSearchSolver.getCost() < lastCost) {
//                        io.Write write = new Write();
//                        write.writeAll(schedule2);
//                        for (int k = 0; k < rawInput.getClassList().size(); k++) {
//                            int[][] schedule3 = new int[weekNum][25];
//                            for (int i = 0; i < schedule.length; i++) {
//                                for (int j = 0; j < schedule[i].length; j++) {
//                                    if (schedule[i][j].getCurSet() != null) {
//                                        for (Cur c3 : schedule[i][j].getCurSet()) {
//                                            if (rawInput.getCurriculumList().get(c3.getSupId()).getClassId().contains(Integer.toString(k + 1))) {
//                                                schedule3[i][j] = c3.getSupId() + 1;
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            write.writeClass(k, schedule3);
//                        }
//                    }

                        System.out.println("---------maybe the true end---------");
                        System.out.println(tabuSearchSolver.getCost());
//                    lastCost = tabuSearchSolver.getCost();
                        if (tabuSearchSolver.CheckSolution(schedule, rawInput.getCurriculumList(), violate) != tabuSearchSolver.getCost()) {
                            System.out.println("CheckSolution错误！");
                        } else {
                            System.out.println("CheckSolution正确！");
                        }
                        System.out.println("共耗时" + Duration.between(start, Instant.now()).toMillis() + "ms");
//                    System.exit(0);

                        resultList.add(tabuSearchSolver.getCost());
                        for (int i = 0; i < 5; i++) {
                            resultList.add(violate[i]);
                        }
                        resultList.add(Duration.between(start, Instant.now()).toMillis());

                        result = resultList.toString() + "\n";

                        break;
                    }
//                System.out.println("共耗时" + Duration.between(start, Instant.now()).toMillis() + "ms");
                    writeResult(result);
                }
            }

//        }
    }

    public static List<List<CurSet>> getListForbidden(int weekNum, RawInput rawInput, List<List<Set<Cur>>> curSetsList) {
        // 班级通识课时间占用以及约束2-教师无法排课
        List<List<CurSet>> curSetsForList = new ArrayList<>();
        for (List<Set<Cur>> curSets : curSetsList) {
            List<CurSet> curSetsFor = new ArrayList<>();
            for (Set<Cur> set : curSets) {
                Set<Integer> cursSupId = new HashSet<>();
                int flag = 0; // 标记三课时课程组
                for (Cur cur : set) {
                    cursSupId.add(cur.getSupId());
                    if (cur.getThreeNum() != 0) {
                        flag = 1;
                        break;
                    }
                }
                // 课程组
                List<Curriculum> curriculums = rawInput.getCurriculumList(cursSupId);
                // 班级组&教师组 注意：这里的编号是从1开始的
                List<String> classes = new ArrayList<>();
                List<String> teachers = new ArrayList<>();
                for (Curriculum curriculum : curriculums) {
                    classes.addAll(curriculum.getClassId());
                    teachers.addAll(curriculum.getTeacherId());
                }

                // 构造forbidden数组
                boolean[][] forbidden = new boolean[weekNum][25];
                // 含三课时课程的课程组只可安排至晚上
                if (flag == 1) {
                    for (int i = 0; i < weekNum; i++) {
                        for (int j = 0; j < 25; j++) {
                            if ((j + 1) % 5 != 0) {
                                forbidden[i][j] = true;
                            }
                        }
                    }
                }
                // 相当于将所有教师的isForbidden数组加合
                for (String teacherId : teachers) {
                    boolean[][] forbidden1 = rawInput.getTeacherList().get(Integer.parseInt(teacherId) - 1).getIsForbidden();
                    for (int i = 0; i < forbidden1.length; i++) {
                        for (int j = 0; j < forbidden1[0].length; j++) {
                            forbidden[i][j] = forbidden[i][j] || forbidden1[i][j];
                        }
                    }
                }
                // 班级通识课占用时间段
                for (String classId : classes) {
                    List<Set<Integer>> forbidden2 = rawInput.getClassList().get(Integer.parseInt(classId) - 1).getIsForbidden();
                    for (int i = 0; i < forbidden2.size(); i++) {
                        for (Integer week : forbidden2.get(i)) {
                            forbidden[week - 1][i] = true;
                        }
                    }
                }

//                // 课程组软约束集合
//                ArrayList<Map<String, String>> constraints = new ArrayList<>();
//                for (Curriculum curriculum : curriculums) {
////                    if (curriculum.getConstraint().size() != 0) {
//                    constraints.add(curriculum.getConstraint());
////                    }
//                }

                // 课程教师集合teachers
                CurSet curSet = new CurSet(set, -1, forbidden, teachers);
                curSetsFor.add(curSet);
            }
            curSetsForList.add(curSetsFor);
        }
        return curSetsForList;
    }

    public static void writeResult(String r){
        try{

            File file =new File("D:\\排课\\result_去重优化.txt");

            //if file doesnt exists, then create it
            if(!file.exists()){
                file.createNewFile();
            }

            //true = append file
            FileWriter fileWritter = new FileWriter(file.getName(),true);
            fileWritter.write(r);
            fileWritter.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
