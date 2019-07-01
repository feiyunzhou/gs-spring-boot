package com.learner.messager;

import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Data
@ToString
@Log4j2
public class MessageBody<T> {
    private T data;
}
