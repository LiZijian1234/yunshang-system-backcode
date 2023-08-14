package com.xuecheng.content;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.ws.Action;
import java.util.List;

/**
 * @author zijianLi
 * @create 2023- 04- 30- 17:26
 */
@SpringBootTest
public class TeachplanMapperTests {
    @Autowired
    TeachplanMapper teachplanMapper;

    @Test
    public void teachplanMapperTests(){
        List<TeachplanDto> teachplanDtos =
                teachplanMapper.selectTreeNodes(117L);
        System.out.println(teachplanDtos);
    }


}
