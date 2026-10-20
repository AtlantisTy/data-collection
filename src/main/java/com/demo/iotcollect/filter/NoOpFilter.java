package com.demo.iotcollect.filter;

import com.demo.iotcollect.model.DataRecord;
import org.springframework.stereotype.Component;

/**
 * 空实现：所有采样直接放行。首版链路透传用。
 */
@Component
public class NoOpFilter implements FilterStrategy {

    @Override
    public boolean pass(DataRecord record) {
        return true;
    }
}