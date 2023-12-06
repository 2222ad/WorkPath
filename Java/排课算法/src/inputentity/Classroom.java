package inputentity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Classroom {
    private String RoomType; // 教室类型
    private int RoomNum;   // 教室数量
}
