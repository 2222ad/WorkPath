package tabu;

import gcp.curclass.Cur;
import inputentity.Curriculum;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TabuSearchSolver {
    private final int TABU_Horizon;
    private final int iterations;
    private int weekNum;
    private CurSet[][] BestSchedule;

    private CurSet[][] schedule;
    private double cost;
    private final int NotMove;
    private final int[] startWeek;    // 记录每门课的开课周次0~17
    private static int curSize;    // 记录一周内每门课的次数
    private List<Curriculum> curriculum;
    /**
     * 目标函数的各系数
     * M1 教师在某些周次的某些时间有其他安排，无法排课*********已当作硬约束处理
     *
     * M2 同一教师负责的课程尽量安排在相邻两个时间段进行，此为满足约束3的奖励值，此约束我决定不采取人工观测文件读取方式，采用教师交集 (2)
     * M3 部分课程安排在一定周次内完成全部课时，此为违背约束4的惩罚值 (3)
     * M4 不同课程在一周内具有不同的课时数量上限（如无规定，一门课一周安排两到三次），此为违背约束8的惩罚值【一周排完才知道】 (4)
     * M5 一周有若干课时要安排在晚上，此为违背约束10的惩罚值【一周排完才知道】 (没有)
     * M6 一门课程不要连续多个时间段或连续多天进行，此惩罚值要比同一老师课程相邻的奖励值高一些 (1)
     * M7 相邻周次课程应尽量相同。周次内时间段发生改变就要给惩罚 (5)
     *
     * f1,f2 教师希望在某个时间段上课，均为约束11以及约束1的惩罚值；将课程安排在某个时间段，f1为偏好该时间段时采用的较小惩罚值，f2为不偏好时采用的较大惩罚值
     */
//    private final double M2, M3, M4, M5, M6, M7, f1, f2;
    private final double M2, M3, M4, M6, M7, f1, f2;

    //此处后期可优化为%5==2||4
    private static final List<Integer> M2List = Arrays.asList(2, 4, 7, 9, 12, 14, 17, 19, 22, 24);// M2相邻时间段
    private static final List<Integer> f2List = Arrays.asList(1, 4, 6, 9, 11, 14, 16, 19, 21, 24);// f2不偏好时间段

//    public TabuSearchSolver(int weekNum, int tabuH, int iter, double m2, double m3, double m4, double m5, double m6, double m7, double F1, double F2, List<CurSet> curSets, List<Curriculum> curriculum) {
    public TabuSearchSolver(int weekNum, int tabuH, int iter, double m2, double m3, double m4, double m6, double m7, double F1, double F2, List<CurSet> curSets, List<Curriculum> curriculum) {
        this.weekNum = weekNum;
        BestSchedule = new CurSet[weekNum][25];
        this.TABU_Horizon = tabuH;
        this.iterations = iter;
        this.M2 = m2;
        this.M3 = m3;
        this.M4 = m4;
//        this.M5 = m5;
        this.M6 = m6;
        this.M7 = m7;
        this.f1 = F1;
        this.f2 = F2;
        this.curriculum = curriculum;
        curSize = curriculum.size();

//        GreedySolver greedySolver = new GreedySolver(weekNum, m2, m3, m4, m5, m6, m7, F1, F2, curSets, curriculum);
        GreedySolver greedySolver = new GreedySolver(weekNum, m2, m3, m4, m6, m7, F1, F2, curSets, curriculum);
        greedySolver.solve();
        this.schedule = greedySolver.getSchedule();
        for (CurSet[] sets : schedule) {
            for (int j = 0; j < schedule[0].length; j++) {
                if (sets[j].getCurSet() != null) {
                    for (Cur cur1 : sets[j].getCurSet()) {
                        for (Cur cur2 : sets[j].getCurSet()) {
                            if (cur1.getSupId() != cur2.getSupId() &&
                                    (!Collections.disjoint(curriculum.get(cur1.getSupId()).getClassId(), curriculum.get(cur2.getSupId()).getClassId()) ||
                                            !Collections.disjoint(curriculum.get(cur1.getSupId()).getTeacherId(), curriculum.get(cur2.getSupId()).getTeacherId()))) {
                                System.out.println("图着色过程有误！！！");
                            }
                        }
                    }
                }
            }
        }
        this.cost = greedySolver.getCost();
        double checkCost = greedySolver.calTotalF(this.schedule, curriculum);
        System.out.println("raw cost = " + this.cost);
        System.out.println("check cost = " + checkCost);
        this.cost = checkCost;

        this.NotMove = greedySolver.getNotMove();
        this.startWeek = greedySolver.getStartWeek();
    }

    public TabuSearchSolver solve() {
        // We use 1-0 exchange move
        double BestNCost, NeighborCost;
        int SwapIndexA = -1, SwapIndexB = -1;
        int iteration_number = 0;
        int forceIter = 0;

        int[][] TABU_Matrix = new int[weekNum * 25 + 2][weekNum * 25 + 2];

        double bestSolutionCost = this.cost;
//        int _ = 0;
        while (iteration_number < iterations && forceIter < 1000) {
            System.out.print(" " + forceIter + " : ");
            BestNCost = Double.MAX_VALUE;
            //试图进一步优化达到D^2-D的效果，目前看来较为失败
//            int[][] Matrix=new int[weekNum*25][weekNum*25];
//            int validIndex;
//            for(validIndex=0;validIndex<weekNum*25;validIndex++){
//                if (schedule[validIndex/25][validIndex%25].getCurSetId()>=NotMove) {
//                    break;
//                }
//            }
//            for(int xAxis=0;xAxis< weekNum*25;xAxis++){
//                if (xAxis != validIndex && (((xAxis + 1) % 5 == 0 && (validIndex + 1) % 5 == 0) || ((xAxis + 1) % 5 != 0 && (validIndex + 1) % 5 != 0))) {
//                    // 要么都是三课时，要么都不是
//
//                }
//            }

            // 0和18*25+1代表两个虚拟课程点
            for (int From = 0; From < weekNum * 25; From++) {
                CurSet NowFrom = schedule[From / 25][From % 25];
                for (int To = 0; To < weekNum * 25; To++) {
                    if (From != To && (((From + 1) % 5 == 0 && (To + 1) % 5 == 0) || ((From + 1) % 5 != 0 && (To + 1) % 5 != 0))) {
                        // 要么都是三课时，要么都不是
                        CurSet NowTo = schedule[To / 25][To % 25];

                        if ((NowFrom.getCurSetId() >= 0 && NowFrom.getCurSetId() < NotMove)
                                || (NowTo.getCurSetId() >= 0 && NowTo.getCurSetId() < NotMove)) {
                            continue;
                        }

                        if ((NowFrom.getForbidden() != null && NowFrom.getForbidden()[To / 25][To % 25])
                                || (NowTo.getForbidden() != null && NowTo.getForbidden()[From / 25][From % 25])) {
                            continue;
                        }
//                        // minus and added cost after remove fromNode
//                        double MinusCost1 = calF(NowFrom, PrevFrom, From, 0) + calF(EndFrom, NowFrom, From + 1, 0);
//                        double AddedCost1 = calF(NowFrom, PrevTo, To, 1) + calF(EndTo, NowFrom, To + 1, 0);
//                        // minus and added cost after remove toNode
//                        double MinusCost2 = calF(NowTo, PrevTo, To, 0) + calF(EndTo, NowTo, To + 1, 0);
//                        double AddedCost2 = calF(NowTo, PrevFrom, From, 1) + calF(EndFrom, NowTo, From + 1, 0);
                        // 交换再复原避免出错 flag=0代表原有，1代表将要插入
//                        double MinusCost1 = calF(NowFrom, PrevFrom, From, 0) + calF(EndFrom, NowFrom, From + 1, 0);
//                        double MinusCost2 = calF(NowTo, PrevTo, To, 0) + calF(EndTo, NowTo, To + 1, 0);

                        //TODO 未去重
                        double MinusCost1 = calF(NowFrom, From);
                        double MinusCost2 = calF(NowTo, To);
                        double MinusCost3 = calM7(NowFrom) + calM7(NowTo);
//                        double oldCost = calTotalF(schedule, curriculum);

                        schedule[From / 25][From % 25] = NowTo;
                        schedule[To / 25][To % 25] = NowFrom;
                        int[] swapWeek = new int[curSize];
                        if (NowTo.getCurSet() != null) {
                            for (Cur toCur : NowTo.getCurSet()) {
                                swapWeek[toCur.getSupId()] = startWeek[toCur.getSupId()];
                                if (startWeek[toCur.getSupId()] > From / 25) {
                                    startWeek[toCur.getSupId()] = From / 25;
                                } else if (startWeek[toCur.getSupId()] < From / 25) {// startWeek应该更新为From/25和第二个cur位置取小
                                    int newWeek = schedule.length - 1;
                                    for (int m = 0; m <= From / 25; m++) {
                                        for (int n = 0; n < schedule[0].length; n++) {
                                            if (schedule[m][n].getCurSet() != null) {
                                                for (Cur cur1 : schedule[m][n].getCurSet()) {
                                                    if (cur1.getSupId() == toCur.getSupId()) {
                                                        newWeek = m;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (newWeek == m) {
                                                break;
                                            }
                                        }
                                        if (newWeek == m) {
                                            break;
                                        }
                                    }
                                    startWeek[toCur.getSupId()] = newWeek;
                                }
                            }
                        }
                        if (NowFrom.getCurSet() != null) {
                            for (Cur fromCur : NowFrom.getCurSet()) {
                                swapWeek[fromCur.getSupId()] = startWeek[fromCur.getSupId()];
                                if (startWeek[fromCur.getSupId()] > To / 25) {
                                    startWeek[fromCur.getSupId()] = To / 25;
                                } else if (startWeek[fromCur.getSupId()] < To / 25) {// startWeek应该更新为To/25和第二个cur位置取小
                                    int newWeek = schedule.length - 1;
                                    for (int m = 0; m <= To / 25; m++) {
                                        for (int n = 0; n < schedule[0].length; n++) {
                                            if (schedule[m][n].getCurSet() != null) {
                                                for (Cur cur2 : schedule[m][n].getCurSet()) {
                                                    if (cur2.getSupId() == fromCur.getSupId()) {
                                                        newWeek = m;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (newWeek == m) {
                                                break;
                                            }
                                        }
                                        if (newWeek == m) {
                                            break;
                                        }
                                    }
                                    startWeek[fromCur.getSupId()] = newWeek;
                                }
                            }
                        }

                        // TODO 未去重
                        // 何必如此兴师动众，简单点，我直接重新计算所有涉及的课程的M7，用以修正不就得了
                        double AddedCost1 = calF(NowFrom, To);
                        double AddedCost2 = calF(NowTo, From);
                        double AddedCost3 = calM7(NowFrom) + calM7(NowTo);
//                        double newCost = calTotalF(schedule, curriculum);

                        schedule[From / 25][From % 25] = NowFrom;
                        schedule[To / 25][To % 25] = NowTo;
                        if (NowTo.getCurSet() != null) {
                            for (Cur toCur : NowTo.getCurSet()) {
                                startWeek[toCur.getSupId()] = swapWeek[toCur.getSupId()];
                            }
                        }
                        if (NowFrom.getCurSet() != null) {
                            for (Cur fromCur : NowFrom.getCurSet()) {
                                startWeek[fromCur.getSupId()] = swapWeek[fromCur.getSupId()];
                            }
                        }
                        CurSet PrevFrom, EndFrom, PrevTo, EndTo;
                        if (From == 0) {
                            PrevFrom = new CurSet(null, 0, null, null);
                        } else {
                            PrevFrom = schedule[(From - 1) / 25][(From - 1) % 25];
                        }
                        if (From + 1 == weekNum * 25) {
                            EndFrom = new CurSet(null, weekNum * 25 + 1, null, null);
                        } else {
                            EndFrom = schedule[(From + 1) / 25][(From + 1) % 25];
                        }
                        if (To == 0) {
                            PrevTo = new CurSet(null, 0, null, null);
                        } else {
                            PrevTo = schedule[(To - 1) / 25][(To - 1) % 25];
                        }
                        if (To + 1 == weekNum * 25) {
                            EndTo = new CurSet(null, weekNum * 25 + 1, null, null);
                        } else {
                            EndTo = schedule[(To + 1) / 25][(To + 1) % 25];
                        }
                        // Check if the move is a Tabu!
                        if ((TABU_Matrix[PrevFrom.getCurSetId()][NowTo.getCurSetId()] != 0)
                                || (TABU_Matrix[NowTo.getCurSetId()][EndFrom.getCurSetId()] != 0)
                                || (TABU_Matrix[PrevTo.getCurSetId()][NowFrom.getCurSetId()] != 0)
                                || (TABU_Matrix[NowFrom.getCurSetId()][EndTo.getCurSetId()] != 0)) {
                            break;
                        }

                        //TODO 未去重
                        NeighborCost = AddedCost1 + AddedCost2 + AddedCost3 - MinusCost1 - MinusCost2 - MinusCost3;
//                          NeighborCost = newCost - oldCost;

//                        if (newCost - oldCost != NeighborCost) {
//                            System.out.println(1);
//                        }

                        // ensure the solution is valid
                        if (NeighborCost < BestNCost) {
                            BestNCost = NeighborCost;
                            SwapIndexA = From;
                            SwapIndexB = To;
                        }
                    }
                }
            }

            for (int o = 0; o < TABU_Matrix[0].length; o++) {
                for (int p = 0; p < TABU_Matrix[0].length; p++) {
                    if (TABU_Matrix[o][p] > 0) {
                        TABU_Matrix[o][p]--;
                    }
                }
            }

            CurSet PrevFrom, NowFrom, EndFrom;
            CurSet PrevTo, NowTo, EndTo;
            if (SwapIndexA == 0) {
                PrevFrom = new CurSet(null, 0, null, null);
            } else {
                PrevFrom = schedule[(SwapIndexA - 1) / 25][(SwapIndexA - 1) % 25];
            }
            NowFrom = schedule[SwapIndexA / 25][SwapIndexA % 25];
            if (SwapIndexA + 1 == weekNum * 25) {
                EndFrom = new CurSet(null, weekNum * 25 + 1, null, null);
            } else {
                EndFrom = schedule[(SwapIndexA + 1) / 25][(SwapIndexA + 1) % 25];
            }
            if (SwapIndexB == 0) {
                PrevTo = new CurSet(null, 0, null, null);
            } else {
                PrevTo = schedule[(SwapIndexB - 1) / 25][(SwapIndexB - 1) % 25];
            }
            NowTo = schedule[SwapIndexB / 25][SwapIndexB % 25];
            if (SwapIndexB + 1 == weekNum * 25) {
                EndTo = new CurSet(null, weekNum * 25 + 1, null, null);
            } else {
                EndTo = schedule[(SwapIndexB + 1) / 25][(SwapIndexB + 1) % 25];
            }

//            Random TabuRan = new Random();
//            int randomDelay1 = TabuRan.nextInt(5);
//            int randomDelay2 = TabuRan.nextInt(5);
//            int randomDelay3 = TabuRan.nextInt(5);
//            int randomDelay4 = TabuRan.nextInt(5);
//
//            TABU_Matrix[PrevFrom.getCurSetId()][NowFrom.getCurSetId()] = this.TABU_Horizon + randomDelay1;
//            TABU_Matrix[NowFrom.getCurSetId()][EndFrom.getCurSetId()] = this.TABU_Horizon + randomDelay2;
//            TABU_Matrix[PrevTo.getCurSetId()][NowTo.getCurSetId()] = this.TABU_Horizon + randomDelay3;
//            TABU_Matrix[NowTo.getCurSetId()][EndTo.getCurSetId()] = this.TABU_Horizon + randomDelay4;
            TABU_Matrix[PrevFrom.getCurSetId()][NowFrom.getCurSetId()] = this.TABU_Horizon;
            TABU_Matrix[NowFrom.getCurSetId()][EndFrom.getCurSetId()] = this.TABU_Horizon;
            TABU_Matrix[PrevTo.getCurSetId()][NowTo.getCurSetId()] = this.TABU_Horizon;
            TABU_Matrix[NowTo.getCurSetId()][EndTo.getCurSetId()] = this.TABU_Horizon;

            schedule[SwapIndexA / 25][SwapIndexA % 25] = NowTo;
            schedule[SwapIndexB / 25][SwapIndexB % 25] = NowFrom;

            int AX = SwapIndexA / 25;
            int BX = SwapIndexB / 25;
            if (NowTo.getCurSet() != null) {
                for (Cur toCur : NowTo.getCurSet()) {
                    if (startWeek[toCur.getSupId()] > AX) {
                        startWeek[toCur.getSupId()] = AX;
                    } else if (startWeek[toCur.getSupId()] < AX) {
                        int newWeek = schedule.length - 1;
                        for (int m = 0; m <= AX; m++) {
                            for (int n = 0; n < schedule[0].length; n++) {
                                if (schedule[m][n].getCurSet() != null) {
                                    for (Cur cur1 : schedule[m][n].getCurSet()) {
                                        if (cur1.getSupId() == toCur.getSupId()) {
                                            newWeek = m;
                                            break;
                                        }
                                    }
                                }
                                if (newWeek == m) {
                                    break;
                                }
                            }
                            if (newWeek == m) {
                                break;
                            }
                        }
                        startWeek[toCur.getSupId()] = newWeek;
                    }
                }
            }
            if (NowFrom.getCurSet() != null) {
                for (Cur fromCur : NowFrom.getCurSet()) {
                    if (startWeek[fromCur.getSupId()] > BX) {
                        startWeek[fromCur.getSupId()] = BX;
                    } else if (startWeek[fromCur.getSupId()] < BX) {
                        int newWeek = schedule.length - 1;
                        for (int m = 0; m <= BX; m++) {
                            for (int n = 0; n < schedule[0].length; n++) {
                                if (schedule[m][n].getCurSet() != null) {
                                    for (Cur cur2 : schedule[m][n].getCurSet()) {
                                        if (cur2.getSupId() == fromCur.getSupId()) {
                                            newWeek = m;
                                            break;
                                        }
                                    }
                                }
                                if (newWeek == m) {
                                    break;
                                }
                            }
                            if (newWeek == m) {
                                break;
                            }
                        }
                        startWeek[fromCur.getSupId()] = newWeek;
                    }
                }
            }

            System.out.println(BestNCost);
            this.cost += BestNCost;
//            writeResult(String.valueOf(this.cost) + "\n");

//            double aaa = calTotalF(schedule, curriculum);
//            if (this.cost != aaa) {
//                System.out.println("wrong");
//            }

            if (this.cost < bestSolutionCost) {
                iteration_number = 0;
                this.BestSchedule = this.schedule;
                bestSolutionCost = this.cost;
            } else {
                iteration_number++;
            }
            forceIter++;
//            _++;
        }
        this.schedule = this.BestSchedule;
        this.cost = bestSolutionCost;
//        double aaa = calTotalF(schedule, curriculum);
//        if (this.cost != aaa) {
//            System.out.println("wrong");
//        }

        return this;
    }

//    public TabuSearchSolver solve() {
//        // We use 1-0 exchange move
//        double BestNCost, NeighborCost;
//        int SwapIndexA = -1, SwapIndexB = -1;
//        int iteration_number = 0;
//        int forceIter = 0;
//
//        int[][] TABU_Matrix = new int[weekNum * 25 + 2][weekNum * 25 + 2];
//
//        double bestSolutionCost = this.cost;
//
//        while (iteration_number < iterations && forceIter < 1000) {
//            BestNCost = Double.MAX_VALUE;
//            int iter0 = 0;
//            //试图进一步优化达到D^2-D的效果，目前看来较为失败
////            int[][] Matrix=new int[weekNum*25][weekNum*25];
////            int validIndex;
////            for(validIndex=0;validIndex<weekNum*25;validIndex++){
////                if (schedule[validIndex/25][validIndex%25].getCurSetId()>=NotMove) {
////                    break;
////                }
////            }
////            for(int xAxis=0;xAxis< weekNum*25;xAxis++){
////                if (xAxis != validIndex && (((xAxis + 1) % 5 == 0 && (validIndex + 1) % 5 == 0) || ((xAxis + 1) % 5 != 0 && (validIndex + 1) % 5 != 0))) {
////                    // 要么都是三课时，要么都不是
////
////                }
////            }
//
//            // 0和18*25+1代表两个虚拟课程点
//            for (int From = 0; From < weekNum * 25 && iter0 < 10; From++) {
//                CurSet NowFrom = schedule[From / 25][From % 25];
//                for (int To = 0; To < weekNum * 25 && iter0 < 10; To++) {
//                    if (From != To && (((From + 1) % 5 == 0 && (To + 1) % 5 == 0) || ((From + 1) % 5 != 0 && (To + 1) % 5 != 0))) {
//                        // 要么都是三课时，要么都不是
//                        CurSet NowTo = schedule[To / 25][To % 25];
//
//                        if ((NowFrom.getCurSetId() >= 0 && NowFrom.getCurSetId() < NotMove)
//                                || (NowTo.getCurSetId() >= 0 && NowTo.getCurSetId() < NotMove)) {
//                            continue;
//                        }
//
//                        if ((NowFrom.getForbidden() != null && NowFrom.getForbidden()[To / 25][To % 25])
//                                || (NowTo.getForbidden() != null && NowTo.getForbidden()[From / 25][From % 25])) {
//                            continue;
//                        }
////                        // minus and added cost after remove fromNode
////                        double MinusCost1 = calF(NowFrom, PrevFrom, From, 0) + calF(EndFrom, NowFrom, From + 1, 0);
////                        double AddedCost1 = calF(NowFrom, PrevTo, To, 1) + calF(EndTo, NowFrom, To + 1, 0);
////                        // minus and added cost after remove toNode
////                        double MinusCost2 = calF(NowTo, PrevTo, To, 0) + calF(EndTo, NowTo, To + 1, 0);
////                        double AddedCost2 = calF(NowTo, PrevFrom, From, 1) + calF(EndFrom, NowTo, From + 1, 0);
//                        // 交换再复原避免出错 flag=0代表原有，1代表将要插入
////                        double MinusCost1 = calF(NowFrom, PrevFrom, From, 0) + calF(EndFrom, NowFrom, From + 1, 0);
////                        double MinusCost2 = calF(NowTo, PrevTo, To, 0) + calF(EndTo, NowTo, To + 1, 0);
//                        double MinusCost1 = calF(NowFrom, From);
//                        double MinusCost2 = calF(NowTo, To);
//                        double MinusCost3 = calM7(NowFrom) + calM7(NowTo);
////                        double oldCost = calTotalF(schedule, curriculum);
//                        schedule[From / 25][From % 25] = NowTo;
//                        schedule[To / 25][To % 25] = NowFrom;
//                        int[] swapWeek = new int[curSize];
//                        if (NowTo.getCurSet() != null) {
//                            for (Cur toCur : NowTo.getCurSet()) {
//                                swapWeek[toCur.getSupId()] = startWeek[toCur.getSupId()];
//                                if (startWeek[toCur.getSupId()] > From / 25) {
//                                    startWeek[toCur.getSupId()] = From / 25;
//                                } else if (startWeek[toCur.getSupId()] < From / 25) {// startWeek应该更新为From/25和第二个cur位置取小
//                                    int newWeek = schedule.length - 1;
//                                    for (int m = 0; m <= From / 25; m++) {
//                                        for (int n = 0; n < schedule[0].length; n++) {
//                                            if (schedule[m][n].getCurSet() != null) {
//                                                for (Cur cur1 : schedule[m][n].getCurSet()) {
//                                                    if (cur1.getSupId() == toCur.getSupId()) {
//                                                        newWeek = m;
//                                                        break;
//                                                    }
//                                                }
//                                            }
//                                            if (newWeek == m) {
//                                                break;
//                                            }
//                                        }
//                                        if (newWeek == m) {
//                                            break;
//                                        }
//                                    }
//                                    startWeek[toCur.getSupId()] = newWeek;
//                                }
//                            }
//                        }
//                        if (NowFrom.getCurSet() != null) {
//                            for (Cur fromCur : NowFrom.getCurSet()) {
//                                swapWeek[fromCur.getSupId()] = startWeek[fromCur.getSupId()];
//                                if (startWeek[fromCur.getSupId()] > To / 25) {
//                                    startWeek[fromCur.getSupId()] = To / 25;
//                                } else if (startWeek[fromCur.getSupId()] < To / 25) {// startWeek应该更新为To/25和第二个cur位置取小
//                                    int newWeek = schedule.length - 1;
//                                    for (int m = 0; m <= To / 25; m++) {
//                                        for (int n = 0; n < schedule[0].length; n++) {
//                                            if (schedule[m][n].getCurSet() != null) {
//                                                for (Cur cur2 : schedule[m][n].getCurSet()) {
//                                                    if (cur2.getSupId() == fromCur.getSupId()) {
//                                                        newWeek = m;
//                                                        break;
//                                                    }
//                                                }
//                                            }
//                                            if (newWeek == m) {
//                                                break;
//                                            }
//                                        }
//                                        if (newWeek == m) {
//                                            break;
//                                        }
//                                    }
//                                    startWeek[fromCur.getSupId()] = newWeek;
//                                }
//                            }
//                        }
//
//                        // 何必如此兴师动众，简单点，我直接重新计算所有涉及的课程的M7，用以修正不就得了
//                        double AddedCost1 = calF(NowFrom, To);
//                        double AddedCost2 = calF(NowTo, From);
//                        double AddedCost3 = calM7(NowFrom) + calM7(NowTo);
////                        double newCost = calTotalF(schedule, curriculum);
//                        schedule[From / 25][From % 25] = NowFrom;
//                        schedule[To / 25][To % 25] = NowTo;
//                        if (NowTo.getCurSet() != null) {
//                            for (Cur toCur : NowTo.getCurSet()) {
//                                startWeek[toCur.getSupId()] = swapWeek[toCur.getSupId()];
//                            }
//                        }
//                        if (NowFrom.getCurSet() != null) {
//                            for (Cur fromCur : NowFrom.getCurSet()) {
//                                startWeek[fromCur.getSupId()] = swapWeek[fromCur.getSupId()];
//                            }
//                        }
//                        CurSet PrevFrom, EndFrom, PrevTo, EndTo;
//                        if (From == 0) {
//                            PrevFrom = new CurSet(null, 0, null, null);
//                        } else {
//                            PrevFrom = schedule[(From - 1) / 25][(From - 1) % 25];
//                        }
//                        if (From + 1 == weekNum * 25) {
//                            EndFrom = new CurSet(null, weekNum * 25 + 1, null, null);
//                        } else {
//                            EndFrom = schedule[(From + 1) / 25][(From + 1) % 25];
//                        }
//                        if (To == 0) {
//                            PrevTo = new CurSet(null, 0, null, null);
//                        } else {
//                            PrevTo = schedule[(To - 1) / 25][(To - 1) % 25];
//                        }
//                        if (To + 1 == weekNum * 25) {
//                            EndTo = new CurSet(null, weekNum * 25 + 1, null, null);
//                        } else {
//                            EndTo = schedule[(To + 1) / 25][(To + 1) % 25];
//                        }
//                        // Check if the move is a Tabu!
//                        if ((TABU_Matrix[PrevFrom.getCurSetId()][NowTo.getCurSetId()] != 0)
//                                || (TABU_Matrix[NowTo.getCurSetId()][EndFrom.getCurSetId()] != 0)
//                                || (TABU_Matrix[PrevTo.getCurSetId()][NowFrom.getCurSetId()] != 0)
//                                || (TABU_Matrix[NowFrom.getCurSetId()][EndTo.getCurSetId()] != 0)) {
//                            break;
//                        }
//
//                        NeighborCost = AddedCost1 + AddedCost2 + AddedCost3 - MinusCost1 - MinusCost2 - MinusCost3;
//
////##                        NeighborCost = newCost - oldCost;
////                        if (newCost - oldCost != NeighborCost) {
////                            System.out.println(1);
////                        }
//
//                        // ensure the solution is valid
//                        if (NeighborCost < BestNCost) {
//                            BestNCost = NeighborCost;
//                            SwapIndexA = From;
//                            SwapIndexB = To;
//                            iter0++;
//                        }
//                    }
//                }
//            }
//
//            for (int o = 0; o < TABU_Matrix[0].length; o++) {
//                for (int p = 0; p < TABU_Matrix[0].length; p++) {
//                    if (TABU_Matrix[o][p] > 0) {
//                        TABU_Matrix[o][p]--;
//                    }
//                }
//            }
//
//            CurSet PrevFrom, NowFrom, EndFrom;
//            CurSet PrevTo, NowTo, EndTo;
//            if (SwapIndexA == 0) {
//                PrevFrom = new CurSet(null, 0, null, null);
//            } else {
//                PrevFrom = schedule[(SwapIndexA - 1) / 25][(SwapIndexA - 1) % 25];
//            }
//            NowFrom = schedule[SwapIndexA / 25][SwapIndexA % 25];
//            if (SwapIndexA + 1 == weekNum * 25) {
//                EndFrom = new CurSet(null, weekNum * 25 + 1, null, null);
//            } else {
//                EndFrom = schedule[(SwapIndexA + 1) / 25][(SwapIndexA + 1) % 25];
//            }
//            if (SwapIndexB == 0) {
//                PrevTo = new CurSet(null, 0, null, null);
//            } else {
//                PrevTo = schedule[(SwapIndexB - 1) / 25][(SwapIndexB - 1) % 25];
//            }
//            NowTo = schedule[SwapIndexB / 25][SwapIndexB % 25];
//            if (SwapIndexB + 1 == weekNum * 25) {
//                EndTo = new CurSet(null, weekNum * 25 + 1, null, null);
//            } else {
//                EndTo = schedule[(SwapIndexB + 1) / 25][(SwapIndexB + 1) % 25];
//            }
//
//            Random TabuRan = new Random();
//            int randomDelay1 = TabuRan.nextInt(5);
//            int randomDelay2 = TabuRan.nextInt(5);
//            int randomDelay3 = TabuRan.nextInt(5);
//            int randomDelay4 = TabuRan.nextInt(5);
//
//            TABU_Matrix[PrevFrom.getCurSetId()][NowFrom.getCurSetId()] = this.TABU_Horizon + randomDelay1;
//            TABU_Matrix[NowFrom.getCurSetId()][EndFrom.getCurSetId()] = this.TABU_Horizon + randomDelay2;
//            TABU_Matrix[PrevTo.getCurSetId()][NowTo.getCurSetId()] = this.TABU_Horizon + randomDelay3;
//            TABU_Matrix[NowTo.getCurSetId()][EndTo.getCurSetId()] = this.TABU_Horizon + randomDelay4;
////            TABU_Matrix[PrevFrom.getCurSetId()][NowFrom.getCurSetId()] = this.TABU_Horizon;
////            TABU_Matrix[NowFrom.getCurSetId()][EndFrom.getCurSetId()] = this.TABU_Horizon;
////            TABU_Matrix[PrevTo.getCurSetId()][NowTo.getCurSetId()] = this.TABU_Horizon;
////            TABU_Matrix[NowTo.getCurSetId()][EndTo.getCurSetId()] = this.TABU_Horizon;
//
//            schedule[SwapIndexA / 25][SwapIndexA % 25] = NowTo;
//            schedule[SwapIndexB / 25][SwapIndexB % 25] = NowFrom;
//
//            int AX = SwapIndexA / 25;
//            int BX = SwapIndexB / 25;
//            if (NowTo.getCurSet() != null) {
//                for (Cur toCur : NowTo.getCurSet()) {
//                    if (startWeek[toCur.getSupId()] > AX) {
//                        startWeek[toCur.getSupId()] = AX;
//                    } else if (startWeek[toCur.getSupId()] < AX) {
//                        int newWeek = schedule.length - 1;
//                        for (int m = 0; m <= AX; m++) {
//                            for (int n = 0; n < schedule[0].length; n++) {
//                                if (schedule[m][n].getCurSet() != null) {
//                                    for (Cur cur1 : schedule[m][n].getCurSet()) {
//                                        if (cur1.getSupId() == toCur.getSupId()) {
//                                            newWeek = m;
//                                            break;
//                                        }
//                                    }
//                                }
//                                if (newWeek == m) {
//                                    break;
//                                }
//                            }
//                            if (newWeek == m) {
//                                break;
//                            }
//                        }
//                        startWeek[toCur.getSupId()] = newWeek;
//                    }
//                }
//            }
//            if (NowFrom.getCurSet() != null) {
//                for (Cur fromCur : NowFrom.getCurSet()) {
//                    if (startWeek[fromCur.getSupId()] > BX) {
//                        startWeek[fromCur.getSupId()] = BX;
//                    } else if (startWeek[fromCur.getSupId()] < BX) {
//                        int newWeek = schedule.length - 1;
//                        for (int m = 0; m <= BX; m++) {
//                            for (int n = 0; n < schedule[0].length; n++) {
//                                if (schedule[m][n].getCurSet() != null) {
//                                    for (Cur cur2 : schedule[m][n].getCurSet()) {
//                                        if (cur2.getSupId() == fromCur.getSupId()) {
//                                            newWeek = m;
//                                            break;
//                                        }
//                                    }
//                                }
//                                if (newWeek == m) {
//                                    break;
//                                }
//                            }
//                            if (newWeek == m) {
//                                break;
//                            }
//                        }
//                        startWeek[fromCur.getSupId()] = newWeek;
//                    }
//                }
//            }
//
//            System.out.println(BestNCost);
//            this.cost += BestNCost;
////            double aaa = calTotalF(schedule, curriculum);
////            if (this.cost != aaa) {
////                System.out.println("wrong");
////            }
//
//            if (this.cost < bestSolutionCost) {
//                iteration_number = 0;
//                this.BestSchedule = this.schedule;
//                bestSolutionCost = this.cost;
//            } else {
//                iteration_number++;
//            }
//            forceIter++;
//        }
//        this.schedule = this.BestSchedule;
//        this.cost = bestSolutionCost;
////        double aaa = calTotalF(schedule, curriculum);
////        if (this.cost != aaa) {
////            System.out.println("wrong");
////        }
//
//        return this;
//    }

//    private double calF(CurSet now, CurSet prev, int k, int flag) {
//        if (k == 18 * 25 || now.getCurSet() == null) {
//            return 0;
//        }
//
//        //当前要插入的点
//        int tx = k / 25;
//        int ty = k % 25;
//        CurSet[] week = schedule[tx];
//        CurSet[] day = new CurSet[5];
//        int dayIndex = ty / 5;
//        System.arraycopy(week, dayIndex * 5, day, 0, day.length);
//        double nextF = 0; // 下一个插入的节点所产生的惩罚值
//
//        for (Cur cur : now.getCurSet()) {
//            int M45count = 0, M6count = 0, M10count = 0;
//            for (CurSet curs : week) {
//                if (curs.getCurSet() != null) {
//                    // 比较当前课程集与week内各课程号
//                    for (Cur weekC : curs.getCurSet()) {
//                        if (weekC.getSupId() == cur.getSupId()) {
//                            M45count++;
//                            if (weekC.getThreeNum() == 1 && cur.getThreeNum() == 1) {
//                                M10count++;
//                            }
//                        }
//                    }
//                }
//            }
//            for (CurSet curs : day) {
//                if (curs != null && curs.getCurSet() != null) {
//                    ArrayList<Integer> listB = new ArrayList<>();
//                    for (Cur curSC : curs.getCurSet()) {
//                        listB.add(curSC.getSupId());
//                    }
//                    if (listB.contains(cur.getSupId())) {
//                        M6count++;
//                    }
//                }
//            }
//            String timeslotString = cur.getConstraint() != null ? cur.getConstraint().get("4") : null;
//            if (timeslotString != null) {
//                String[] timeslot = timeslotString.split(",");
//                if (Integer.parseInt(timeslot[0]) > tx + 1 || Integer.parseInt(timeslot[1]) < tx + 1) {
//                    nextF += M3;
//                }
//            }
//
////            String timescale = cur.getConstraint() != null ? cur.getConstraint().get("11") : null;
////            if (timescale != null) {
////                //假如正是他所偏好的
////                if (Integer.parseInt(timescale) == ty + 1) {
////                    nextF += f1;
////                } else if (f2List.contains(ty + 1)) {
////                    nextF += f2;
////                }
////            }
//
//            // 这里得标记一个状态flag，在tabu中你原来占有的0计算M4时，应该是>；而对于新交换1进来的，应该是>=
//            String upper = cur.getConstraint() != null ? cur.getConstraint().get("8") : null;
//            int upper2 = upper == null ? 3 : Integer.parseInt(upper);
//            if (flag == 1) {
//                if (M45count >= upper2) {
//                    nextF += M4;
//                }
//            } else {
//                if (M45count > upper2) {
//                    nextF += M4;
//                }
//            }
//
//
//            String lower = cur.getConstraint() != null ? cur.getConstraint().get("10") : null;
//            if (lower != null) {
//                int lower2 = Integer.parseInt(lower);
//                if (flag == 1) {
//                    if (M10count >= lower2) {
//                        nextF += M5;
//                    }
//                } else if (M10count >= lower2 + 1) {
//                    nextF += M5;
//                }
//            }
//            if (flag == 1) {
//                if (M6count >= 1) {
//                    nextF += M6;
//                }
//            } else {
//                if (M6count > 1) {
//                    nextF += M6;
//                }
//            }
//
////            // 与上周同一时间课程集对比，从第0周到第17周
////            if (startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != tx && tx - 1 >= 0) {
////                ArrayList<Integer> lastCurID = new ArrayList<>();
////                if (schedule[tx - 1][ty].getCurSet() != null) {
////                    for (Cur lastCur : schedule[tx - 1][ty].getCurSet()) {
////                        lastCurID.add(lastCur.getSupId());
////                    }
////                    if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
////                        nextF += M7;
////                    }
////                } else {
////                    nextF += M7;
////                }
////            }
//        }
//        if (prev.getCurSet() != null && M2List.contains(ty)) {  // 相邻时间段的奖励值
//            nextF -= CollectionUtils.intersection(prev.getTeachers(), now.getTeachers()).size() * M2;
//        }
////        //TODO 这里应当补上对下一周产生的影响
////        if (k < 17 * 25) {
////            CurSet nextCurSet = schedule[k / 25 + 1][k % 25];
////            if (nextCurSet.getCurSet() != null) {
////                for (Cur nextCur : nextCurSet.getCurSet()) {
////                    if (startWeek[nextCur.getSupId()] != -1 && startWeek[nextCur.getSupId()] != k / 25 + 1) {
////                        ArrayList<Integer> lastCurID = new ArrayList<>();
////                        if (now.getCurSet() != null) {
////                            for (Cur lastCur : now.getCurSet()) {
////                                lastCurID.add(lastCur.getSupId());
////                            }
////                            if (!lastCurID.contains(nextCur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
////                                nextF += M7;
////                            }
////                        } else {
////                            nextF += M7;
////                        }
////                    }
////                }
////            }
////        }
////        //TODO 如果变成startweek会对后面所有同种课程产生影响
//        return nextF;
//    }

    private double calF(CurSet now, int k) {
        CurSet prev, end;
        if (k == 0) {
            prev = new CurSet(null, 0, null, null);
        } else {
            prev = schedule[(k - 1) / 25][(k - 1) % 25];
        }
        if (k + 1 == weekNum * 25) {
            end = new CurSet(null, weekNum * 25 + 1, null, null);
        } else {
            end = schedule[(k + 1) / 25][(k + 1) % 25];
        }
        if (k == weekNum * 25 || now.getCurSet() == null) {
            return 0;
        }

        //当前要插入的点
        int tx = k / 25;
        int ty = k % 25;
        CurSet[] week = schedule[tx];
        CurSet[] day = new CurSet[5];
        int dayIndex = ty / 5;
        System.arraycopy(week, dayIndex * 5, day, 0, day.length);
        double nextF = 0; // 下一个插入的节点所产生的惩罚值

        for (Cur cur : now.getCurSet()) {
            int M45count = 0, M6count = 0, M10count = 0;
            for (CurSet curs : week) {
                if (curs.getCurSet() != null) {
                    // 比较当前课程集与week内各课程号
                    for (Cur weekC : curs.getCurSet()) {
                        if (weekC.getSupId() == cur.getSupId()) {
                            M45count++;
                            if (weekC.getThreeNum() == 1 && cur.getThreeNum() == 1) {
                                M10count++;
                            }
                        }
                    }
                }
            }
            for (CurSet curs : day) {
                if (curs != null && curs.getCurSet() != null) {
                    ArrayList<Integer> listB = new ArrayList<>();
                    for (Cur curSC : curs.getCurSet()) {
                        listB.add(curSC.getSupId());
                    }
                    if (listB.contains(cur.getSupId())) {
                        M6count++;
                    }
                }
            }
            String timeslotString = cur.getConstraint() != null ? cur.getConstraint().get("4") : null;
            if (timeslotString != null) {
                String[] timeslot = timeslotString.split(",");
                if (Integer.parseInt(timeslot[0]) > tx + 1 || Integer.parseInt(timeslot[1]) < tx + 1) {
                    nextF += M3;
                }
            }


            String upper = cur.getConstraint() != null ? cur.getConstraint().get("8") : null;
            int upper2 = upper == null ? 3 : Integer.parseInt(upper);

            if (M45count > upper2) {
                nextF += M4;
            }


            String lower = cur.getConstraint() != null ? cur.getConstraint().get("10") : null;
            if (lower != null) {
                int lower2 = Integer.parseInt(lower);
                if (M10count >= lower2 + 1) {
//                    nextF += M5;
                }
            }
            if (M6count > 1) {
                nextF += M6;
            }

        }
        if (prev.getTeachers() != null && M2List.contains(ty)) {  // 相邻时间段的奖励值
            nextF -= CollectionUtils.intersection(prev.getTeachers(), now.getTeachers()).size() * M2;
        }
        if (end.getTeachers() != null && M2List.contains(ty + 1)) {  // 相邻时间段的奖励值
            nextF -= CollectionUtils.intersection(end.getTeachers(), now.getTeachers()).size() * M2;
        }
        return nextF;
    }

    public double CheckSolution(CurSet[][] schedule, List<Curriculum> curriculum , int[] violate) {
        double TotalF = 0;
        int N1 = 0, N2 = 0, N3 = 0, N4 = 0, N5 = 0, N6 = 0;
        for (int i = 0; i < schedule.length; i++) {
            int[] sameNum = new int[curSize];
            int[] nightNum = new int[curSize];    // 晚课
            // 用以避免相同cur重复计算惩罚值
            int[] same4 = new int[curSize];
            int[] same5 = new int[curSize];
            int[] same6 = new int[curSize];
            CurSet[] week = schedule[i];
            for (int j = 0; j < schedule[0].length; j++) {
                // 此处存在一个极其隐蔽的bug，same6作为一天内是否出现多门相同课的标记，应该每五天置0
                if (j % 5 == 0) {
                    Arrays.fill(same6, 0);
                }//而且还得放外面，否则会受到下面if语句的限制
                if (schedule[i][j] != null && schedule[i][j].getCurSet() != null) {
                    if (M2List.contains(j) && schedule[i][j - 1].getTeachers() != null && schedule[i][j].getTeachers() != null) {  // 相邻时间段的奖励值
                        TotalF -= CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size() * M2;
                        N2 += CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size();
                    }

                    CurSet[] day = new CurSet[5];
                    int dayIndex = j / 5;
                    System.arraycopy(week, dayIndex * 5, day, 0, day.length);

                    for (Cur cur : schedule[i][j].getCurSet()) {
                        sameNum[cur.getSupId()] += 1;
                        if (cur.getThreeNum() == 1) {
                            nightNum[cur.getSupId()] += 1;
                        }
                        String timeslotString = cur.getConstraint() != null ? cur.getConstraint().get("4") : null;
                        if (timeslotString != null) {
                            String[] timeslot = timeslotString.split(",");
                            if (Integer.parseInt(timeslot[0]) > i + 1 || Integer.parseInt(timeslot[1]) < i + 1) {
                                TotalF += M3;
                                N3 += 1;
                            }
                        }

                        int M6count = 0;
                        for (CurSet curs : day) {
                            if (curs != null && curs.getCurSet() != null) {
                                ArrayList<Integer> listB = new ArrayList<>();
                                for (Cur curSC : curs.getCurSet()) {
                                    listB.add(curSC.getSupId());
                                }
                                if (listB.contains(cur.getSupId())) {
                                    M6count++;
                                }
                            }
                        }
                        if (M6count > 1 && same6[cur.getSupId()] != 1) {
                            TotalF += M6 * (M6count - 1);
                            N1 += M6count - 1;
                            same6[cur.getSupId()] = 1;
                        }
                        // 与上周同一时间课程集对比，从第0周到第17周
                        if (startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != i && i - 1 >= 0) {
                            ArrayList<Integer> lastCurID = new ArrayList<>();
                            if (schedule[i - 1][j].getCurSet() != null) {
                                for (Cur lastCur : schedule[i - 1][j].getCurSet()) {
                                    lastCurID.add(lastCur.getSupId());
                                }
                                if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
                                    TotalF += M7;
                                    N6 += 1;
                                }
                            } else {
                                TotalF += M7;
                                N6 += 1;
                            }
                        }
                    }
                }
            }
            for (int k = 0; k < curSize; k++) {
                String upper = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("8") : null;
                int upper2 = upper == null ? 3 : Integer.parseInt(upper);
                if (sameNum[k] > upper2 && same4[k] != 1) {
                    TotalF += M4 * (sameNum[k] - upper2);
                    N4 += sameNum[k] - upper2;
                    same4[k] = 1;
                }
                String lower = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("10") : null;
                if (lower != null) {
                    int lower2 = Integer.parseInt(lower);
                    if (nightNum[k] > lower2 && same5[k] != 1) {
//                        TotalF += M5 * (nightNum[k] - lower2);
//                        N5 += nightNum[k] - lower2;
                        same5[k] = 1;
                    }
                }
            }
        }
//        System.out.println("N1违背" + N1 + "次");
//        System.out.println("N2违背" + N2 + "次");
//        System.out.println("N3违背" + N3 + "次");
//        System.out.println("N4违背" + N4 + "次");
//        System.out.println("N5违背" + N5 + "次");
//        System.out.println("N6违背" + N6 + "次");

        violate[0] = N1;
        violate[1] = N2;
        violate[2] = N3;
        violate[3] = N4;
//        violate[4] = N5;
        violate[4] = N6;

        return TotalF;
    }
//    public double CheckSolution(CurSet[][] schedule, List<Curriculum> curriculum) {
//        double TotalF = 0;
//        int N1 = 0, N2 = 0, N3 = 0, N4 = 0, N5 = 0, N6 = 0;
//        for (int i = 0; i < schedule.length; i++) {
//            int[] sameNum = new int[curSize];
//            int[] nightNum = new int[curSize];    // 晚课
//            // 用以避免相同cur重复计算惩罚值
//            int[] same4 = new int[curSize];
//            int[] same5 = new int[curSize];
//            int[] same6 = new int[curSize];
//            CurSet[] week = schedule[i];
//            for (int j = 0; j < schedule[0].length; j++) {
//                // 此处存在一个极其隐蔽的bug，same6作为一天内是否出现多门相同课的标记，应该每五天置0
//                if (j % 5 == 0) {
//                    Arrays.fill(same6, 0);
//                }//而且还得放外面，否则会受到下面if语句的限制
//                if (schedule[i][j] != null && schedule[i][j].getCurSet() != null) {
//                    if (M2List.contains(j) && schedule[i][j - 1].getTeachers() != null && schedule[i][j].getTeachers() != null) {  // 相邻时间段的奖励值
//                        TotalF -= CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size() * M2;
//                        N2 += CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size();
//                    }
//
//                    CurSet[] day = new CurSet[5];
//                    int dayIndex = j / 5;
//                    System.arraycopy(week, dayIndex * 5, day, 0, day.length);
//
//                    for (Cur cur : schedule[i][j].getCurSet()) {
//                        sameNum[cur.getSupId()] += 1;
//                        if (cur.getThreeNum() == 1) {
//                            nightNum[cur.getSupId()] += 1;
//                        }
//                        String timeslotString = cur.getConstraint() != null ? cur.getConstraint().get("4") : null;
//                        if (timeslotString != null) {
//                            String[] timeslot = timeslotString.split(",");
//                            if (Integer.parseInt(timeslot[0]) > i + 1 || Integer.parseInt(timeslot[1]) < i + 1) {
//                                TotalF += M3;
//                                N3 += 1;
//                            }
//                        }
//
//                        int M6count = 0;
//                        for (CurSet curs : day) {
//                            if (curs != null && curs.getCurSet() != null) {
//                                ArrayList<Integer> listB = new ArrayList<>();
//                                for (Cur curSC : curs.getCurSet()) {
//                                    listB.add(curSC.getSupId());
//                                }
//                                if (listB.contains(cur.getSupId())) {
//                                    M6count++;
//                                }
//                            }
//                        }
//                        if (M6count > 1 && same6[cur.getSupId()] != 1) {
//                            TotalF += M6 * (M6count - 1);
//                            N1 += M6count - 1;
//                            same6[cur.getSupId()] = 1;
//                        }
//                        // 与上周同一时间课程集对比，从第0周到第17周
//                        if (startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != i && i - 1 >= 0) {
//                            ArrayList<Integer> lastCurID = new ArrayList<>();
//                            if (schedule[i - 1][j].getCurSet() != null) {
//                                for (Cur lastCur : schedule[i - 1][j].getCurSet()) {
//                                    lastCurID.add(lastCur.getSupId());
//                                }
//                                if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
//                                    TotalF += M7;
//                                    N6 += 1;
//                                }
//                            } else {
//                                TotalF += M7;
//                                N6 += 1;
//                            }
//                        }
//                    }
//                    for (Cur cur1 : schedule[i][j].getCurSet()) {
//                        for (Cur cur2 : schedule[i][j].getCurSet()) {
//                            if (cur1.getSupId() != cur2.getSupId() &&
//                                    (!Collections.disjoint(curriculum.get(cur1.getSupId()).getClassId(), curriculum.get(cur2.getSupId()).getClassId()) ||
//                                            !Collections.disjoint(curriculum.get(cur1.getSupId()).getTeacherId(), curriculum.get(cur2.getSupId()).getTeacherId()))) {
//                                System.out.println("图着色过程有误！！！");
//                            }
//                        }
//                    }
//                }
//            }
//            for (int k = 0; k < curSize; k++) {
//                String upper = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("8") : null;
//                int upper2 = upper == null ? 3 : Integer.parseInt(upper);
//                if (sameNum[k] > upper2 && same4[k] != 1) {
//                    TotalF += M4 * (sameNum[k] - upper2);
//                    N4 += sameNum[k] - upper2;
//                    same4[k] = 1;
//                }
//                String lower = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("10") : null;
//                if (lower != null) {
//                    int lower2 = Integer.parseInt(lower);
//                    if (nightNum[k] > lower2 && same5[k] != 1) {
//                        TotalF += M5 * (nightNum[k] - lower2);
//                        N5 += nightNum[k] - lower2;
//                        same5[k] = 1;
//                    }
//                }
//            }
//        }
//        System.out.println("N1违背" + N1 + "次");
//        System.out.println("N2违背" + N2 + "次");
//        System.out.println("N3违背" + N3 + "次");
//        System.out.println("N4违背" + N4 + "次");
//        System.out.println("N5违背" + N5 + "次");
//        System.out.println("N6违背" + N6 + "次");
//        return TotalF;
//    }

    public double calTotalF(CurSet[][] schedule, List<Curriculum> curriculum) {
        double TotalF = 0;
        int N1 = 0, N2 = 0, N3 = 0, N4 = 0, N5 = 0, N6 = 0;
        for (int i = 0; i < schedule.length; i++) {
            int[] sameNum = new int[curSize];
            int[] nightNum = new int[curSize];    // 晚课
            // 用以避免相同cur重复计算惩罚值
            int[] same4 = new int[curSize];
            int[] same5 = new int[curSize];
            int[] same6 = new int[curSize];
            CurSet[] week = schedule[i];
            for (int j = 0; j < schedule[0].length; j++) {
                // 此处存在一个极其隐蔽的bug，same6作为一天内是否出现多门相同课的标记，应该每五天置0
                if (j % 5 == 0) {
                    Arrays.fill(same6, 0);
                }//而且还得放外面，否则会受到下面if语句的限制
                if (schedule[i][j] != null && schedule[i][j].getCurSet() != null) {
                    if (M2List.contains(j) && schedule[i][j - 1].getTeachers() != null && schedule[i][j].getTeachers() != null) {  // 相邻时间段的奖励值
                        TotalF -= CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size() * M2;
                        N2 += CollectionUtils.intersection(schedule[i][j - 1].getTeachers(), schedule[i][j].getTeachers()).size();
                    }

                    CurSet[] day = new CurSet[5];
                    int dayIndex = j / 5;
                    System.arraycopy(week, dayIndex * 5, day, 0, day.length);

                    for (Cur cur : schedule[i][j].getCurSet()) {
                        sameNum[cur.getSupId()] += 1;
                        if (cur.getThreeNum() == 1) {
                            nightNum[cur.getSupId()] += 1;
                        }
                        String timeslotString = cur.getConstraint() != null ? cur.getConstraint().get("4") : null;
                        if (timeslotString != null) {
                            String[] timeslot = timeslotString.split(",");
                            if (Integer.parseInt(timeslot[0]) > i + 1 || Integer.parseInt(timeslot[1]) < i + 1) {
                                TotalF += M3;
                                N3 += 1;
                            }
                        }

                        int M6count = 0;
                        for (CurSet curs : day) {
                            if (curs != null && curs.getCurSet() != null) {
                                ArrayList<Integer> listB = new ArrayList<>();
                                for (Cur curSC : curs.getCurSet()) {
                                    listB.add(curSC.getSupId());
                                }
                                if (listB.contains(cur.getSupId())) {
                                    M6count++;
                                }
                            }
                        }
                        if (M6count > 1 && same6[cur.getSupId()] != 1) {
                            TotalF += M6 * (M6count - 1);
                            N1 += M6count - 1;
                            same6[cur.getSupId()] = 1;
                        }
                        // 与上周同一时间课程集对比，从第0周到第17周
                        if (startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != i && i - 1 >= 0) {
                            ArrayList<Integer> lastCurID = new ArrayList<>();
                            if (schedule[i - 1][j].getCurSet() != null) {
                                for (Cur lastCur : schedule[i - 1][j].getCurSet()) {
                                    lastCurID.add(lastCur.getSupId());
                                }
                                if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
                                    TotalF += M7;
                                    N6 += 1;
                                }
                            } else {
                                TotalF += M7;
                                N6 += 1;
                            }
                        }
                    }
                }
            }
            for (int k = 0; k < curSize; k++) {
                String upper = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("8") : null;
                int upper2 = upper == null ? 3 : Integer.parseInt(upper);
                if (sameNum[k] > upper2 && same4[k] != 1) {
                    TotalF += M4 * (sameNum[k] - upper2);
                    N4 += sameNum[k] - upper2;
                    same4[k] = 1;
                }
                String lower = curriculum.get(k).getConstraint() != null ? curriculum.get(k).getConstraint().get("10") : null;
                if (lower != null) {
                    int lower2 = Integer.parseInt(lower);
                    if (nightNum[k] > lower2 && same5[k] != 1) {
//                        TotalF += M5 * (nightNum[k] - lower2);
//                        N5 += nightNum[k] - lower2;
                        same5[k] = 1;
                    }
                }
            }
        }
//        System.out.println("N1违背" + N1 + "次");
//        System.out.println("N2违背" + N2 + "次");
//        System.out.println("N3违背" + N3 + "次");
//        System.out.println("N4违背" + N4 + "次");
//        System.out.println("N5违背" + N5 + "次");
//        System.out.println("N6违背" + N6 + "次");
        return TotalF;
    }

    // 只是单纯用来修正的，如果交换的课时组恰好是startWeek的，且这个startWeek这周仅有这一个课时，这时swapWeek才会有值
    private double calM7(CurSet nowCur) {
        if (nowCur.getCurSet() == null) {
            return 0;
        }
        double correct = 0;
        List<Integer> nowCurId = new ArrayList<>();
        for (Cur nowC : nowCur.getCurSet()) {
            nowCurId.add(nowC.getSupId());
        }
        for (int i = 0; i < schedule.length; i++) {
            for (int j = 0; j < schedule[0].length; j++) {
                if (schedule[i][j] != null && schedule[i][j].getCurSet() != null) {
                    for (Cur cur : schedule[i][j].getCurSet()) {
                        // 与上周同一时间课程集对比，从第0周到第17周
                        if (nowCurId.contains(cur.getSupId()) && startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != i && i - 1 >= 0) {
                            ArrayList<Integer> lastCurID = new ArrayList<>();
                            if (schedule[i - 1][j].getCurSet() != null) {
                                for (Cur lastCur : schedule[i - 1][j].getCurSet()) {
                                    lastCurID.add(lastCur.getSupId());
                                }
                                if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
                                    correct += M7;
                                }
                            } else {
                                correct += M7;
                            }
                        }
                    }
                }

            }
        }
        return correct;
    }

//    // 试图优化，但我懒，有时间再优化M7的计算过程
//    // 只是单纯用来修正的，如果交换的课时组恰好是startWeek的，且这个startWeek这周仅有这一个课时，这时swapWeek才会有值
//    private double calM7Opt(CurSet nowCur,int toIndex) {
//        if (nowCur.getCurSet() == null) {
//            return 0;
//        }
//        double correct = 0;
//        List<Integer> nowCurId = new ArrayList<>();
//        for (Cur nowC : nowCur.getCurSet()) {
//            nowCurId.add(nowC.getSupId());
//        }
//        for (int i = toIndex; i < schedule.length; i++) {
//            for (int j = 0; j < schedule[0].length; j++) {
//                if (schedule[i][j] != null && schedule[i][j].getCurSet() != null) {
//                    for (Cur cur : schedule[i][j].getCurSet()) {
//                        // 与上周同一时间课程集对比，从第0周到第17周
//                        if (nowCurId.contains(cur.getSupId()) && startWeek[cur.getSupId()] != -1 && startWeek[cur.getSupId()] != i && i - 1 >= 0) {
//                            ArrayList<Integer> lastCurID = new ArrayList<>();
//                            if (schedule[i - 1][j].getCurSet() != null) {
//                                for (Cur lastCur : schedule[i - 1][j].getCurSet()) {
//                                    lastCurID.add(lastCur.getSupId());
//                                }
//                                if (!lastCurID.contains(cur.getSupId())) {  // 如果上周同一时间段没有这门课，给予惩罚
//                                    correct += M7;
//                                }
//                            } else {
//                                correct += M7;
//                            }
//                        }
//                    }
//                }
//
//            }
//        }
//        return correct;
//    }

    public CurSet[][] getSchedule() {
        return schedule;
    }

    public double getCost() {
        return cost;
    }

    public void writeResult(String r){
        try{

            File file =new File("D:\\桌面文件\\论文\\排课\\算法\\排课算法-去重\\result_收敛图_大算例.txt");

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
