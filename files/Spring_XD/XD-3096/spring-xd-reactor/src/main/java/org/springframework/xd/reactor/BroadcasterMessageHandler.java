/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.reactor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import reactor.Environment;
import reactor.fn.Consumer;
import reactor.rx.Stream;
import reactor.rx.action.Control;
import reactor.rx.broadcast.Broadcaster;
import reactor.rx.broadcast.SerializedBroadcaster;

import java.lang.reflect.Method;

/**
 * Adapts the item at a time delivery of a {@link org.springframework.messaging.MessageHandler}
 * by delegating processing to a {@link Stream}
 * <p/>
 * The outputStream of the processor is used to create a message and send it to the output channel. If the
 * input channel and output channel are connected to the MessageBus, then data delivered to the input stream via
 * a call to onNext is invoked on the dispatcher thread of the message bus and sending a message to the output
 * channel will involve IO operations on the message bus.
 * <p/>
 * The implementation uses a {@link reactor.rx.broadcast.SerializedBroadcaster} with synchronous dispatch.
 * This has the advantage that the state of the Stream can be shared across all the incoming dispatcher threads that
 * are invoking onNext. It has the disadvantage that processing and sending to the output channel will execute serially
 * on one of the dispatcher threads.
 * <p/>
 * The use of this handler makes for a very natural first experience when processing data. For example given
 * the stream <code></code>http | reactor-processor | log</code> where the <code>reactor-processor</code> does does a
 * <code>buffer(5)</code> and then produces a single value. Sending 10 messages to the http source will
 * result in 2 messages in the log, no matter how many dispatcher threads are used.
 * <p/>
 * You can modify what thread the outputStream subscriber, which does the send to the output channel,
 * will use by explicitly calling <code>observeOn</code> before returning the outputStream from your processor.
 * <p/>
 * Use {@link org.springframework.xd.reactor.MultipleBroadcasterMessageHandler} for concurrent execution on dispatcher
 * threads spread across across multiple Observables.
 * <p/>
 * All error handling is the responsibility of the processor implementation.
 *
 * @author Mark Pollack
 */
public class BroadcasterMessageHandler extends AbstractMessageProducingHandler {

    protected final Log logger = LogFactory.getLog(getClass());

    private final Broadcaster<Object> stream;

    @SuppressWarnings("rawtypes")
    private final Processor reactorProcessor;

    private final ResolvableType inputType;

    private final Control control;

    /**
     * Construct a new BroadcasterMessageHandler given the reactor based Processor to delegate
     * processing to.
     *
     * @param processor The stream based reactor processor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BroadcasterMessageHandler(Processor processor) {
        Assert.notNull(processor, "processor cannot be null.");
        this.reactorProcessor = processor;
        Environment.initializeIfEmpty(); // This by default uses SynchronousDispatcher
        Method method = ReflectionUtils.findMethod(this.reactorProcessor.getClass(), "process", Stream.class);
        this.inputType = ResolvableType.forMethodParameter(method, 0).getNested(2);

        //Stream with a SynchronousDispatcher as this handler is called by Message Listener managed threads
        this.stream = SerializedBroadcaster.create();

        //user defined stream processing
        Stream<?> outputStream = processor.process(stream);

        //Simple log error handling
        outputStream.when(Throwable.class, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                logger.error(throwable);
            }
        });

        this.control = outputStream.consume(new Consumer<Object>() {
            @Override
            public void accept(Object outputObject) {
                if (ClassUtils.isAssignable(Message.class, outputObject.getClass())) {
                    getOutputChannel().send((Message) outputObject);
                } else {
                    getOutputChannel().send(MessageBuilder.withPayload(outputObject).build());
                }
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug(control.debug());
        }

    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {

        if (ClassUtils.isAssignable(inputType.getRawClass(), message.getClass())) {
            stream.onNext(message);
        } else if (ClassUtils.isAssignable(inputType.getRawClass(), message.getPayload().getClass())) {
            //TODO handle type conversion of payload to input type if possible
            stream.onNext(message.getPayload());
        }

        if (logger.isDebugEnabled()) {
            logger.debug(control.debug());
        }
    }

}