package com.rickgao.careercore.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskUtilTest {

    @Test
    void maskPhone_11位保留前3后4() {
        assertEquals("138****5678", MaskUtil.maskPhone("13812345678"));
    }

    @Test
    void maskPhone_长度不足原样返回() {
        assertEquals("123", MaskUtil.maskPhone("123"));
        assertEquals("123456", MaskUtil.maskPhone("123456"));
    }

    @Test
    void maskPhone_空值返回空() {
        assertNull(MaskUtil.maskPhone(null));
    }

    // Demo 精简点:当前仅掩 phone;带 +86 前缀不剥离(后续迭代替换位置)
    @Test
    void maskPhone_带国家码不剥离前缀() {
        assertEquals("+86****4789", MaskUtil.maskPhone("+861381234789"));
    }
}
