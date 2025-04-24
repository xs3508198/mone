package run.mone.mcp.ticket12306;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import run.mone.mcp.ticket12306.function.Ticket12306Function;
import run.mone.mcp.ticket12306.model.TicketInfo;

import java.io.IOException;

@SpringBootTest
class Mcp12306ApplicationTests {

    @Autowired
    private Gson gson;

    @Test
    void test1() {
        String t = "p6pzyu8CGkAopR3ROGiWuoibpEUfHwc322D850WaISgP%2FIVOobHOKI%2Fwlb3TsTS2Gizj%2FVTKhZ8r%0AhqrfXHmsgRtV%2FjtoJubWamMppj4tzEB9aLK3OOnPP81wkrrgqJcLJ4VjRVy2xWCAl0ZMRYLTzXiT%0AdT8ENkJshk4j%2B5Pc0mEtE0BISrdur2AWmwOnHAYjzfZiKp%2BzWDZlHJDF1X7bBh94onyEjDBI9FPZ%0AVNEsyC0jxLMFuAL%2B7g4MLt3eudc2nJmHbG6ab5xVfWwOVood31ZHLvHlyxC8hGAosluzIh5jZQzI%0ABMPi2Ho2bGp%2FxJlD%2B16APblbR4TCK2Z9c9xMV%2Fs3kHU%3D|预订|240000G1593J|G159|VNP|AOH|VNP|AOH|17:19|23:18|05:59|Y|d1XNv3HUfK%2BUClfEj2KyJbUF1%2Flpeuk3IAUXjEnsqu7euFO9|20250424|3|P4|01|11|1|0|||||||||||无|4|7||90M0O0|9MO|0|1||9174800007M088400004O052600000|0|||||1|5#1#0#0#z#0#z#z||7|CHN,CHN|||N#N#||90076M0084O0080|202504101230|Y|";
        TicketInfo info = TicketInfo.parse(t);
        System.out.println(gson.toJson(info));
    }

    @Test
    void test2() throws Exception {
        Ticket12306Function function = new Ticket12306Function();
        function.initStationCode();
    }

}
