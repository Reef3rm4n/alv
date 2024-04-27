package io.alv.core.handler.messages.encoding;

import io.alv.core.Encoding;
import io.alv.core.handler.exceptions.DeserializationException;
import io.alv.core.handler.exceptions.SerializationException;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import org.agrona.MutableDirectBuffer;


public final class MessageEnvelopeCodec {
    private MessageEnvelopeCodec() {
    }

    private static final ThreadLocal<MessageEnvelopeCodecContext> context = ThreadLocal.withInitial(MessageEnvelopeCodecContext::new);

    public static void clean() {
        context.remove();
    }


    public static <T> MessageEnvelope serialize(T message) throws SerializationException {
        return serialize(context.get().messageEnvelopeBuffer(), 0, message);
    }

    public static <T> T deserialize(MessageEnvelope messageEnvelope) throws DeserializationException {
        final Class<T> clazz = (Class<T>) resolveClassType(messageEnvelope);
        try {
            return switch (messageEnvelope.payloadEncoding()) {
                case SBE -> clazz.cast(resolveSbeEncoder(messageEnvelope.payloadType().hashCode())
                        .decode(
                                messageEnvelope.payloadBuffer(),
                                messageEnvelope.offset(),
                                messageEnvelope.length(),
                          null
                        )
                );
                case FURY -> context.get().fury.deserialize(
                        messageEnvelope.payloadBuffer(),
                        messageEnvelope.offset(),
                        messageEnvelope.length(),
                        clazz
                );
                case KYRO -> context.get().kyro.deserialize(
                        messageEnvelope.payloadBuffer(),
                        messageEnvelope.offset(),
                        messageEnvelope.length(),
                        clazz
                );
                case JSON -> context.get().json.deserialize(
                        messageEnvelope.payloadBuffer(),
                        messageEnvelope.offset(),
                        messageEnvelope.length(),
                        clazz
                );
                case NULL_VAL -> throw new IllegalArgumentException("NULL_VAL");
            };
        } catch (Exception e) {
            throw new DeserializationException(messageEnvelope, e);
        }
    }

    private static Class<?> resolveClassType(MessageEnvelope messageEnvelope) {
        return context.get().messageTypesCache.getOrDefault(
                messageEnvelope.payloadType().hashCode(),
                defaultValue(messageEnvelope)
        );
    }

    private static Class<?> defaultValue(MessageEnvelope messageEnvelope) {
        final var clazz = context.get().messageTypes.getOrDefault(messageEnvelope.payloadType().hashCode(), classForName(messageEnvelope));
        context.get().messageTypesCache.put(messageEnvelope.payloadType().hashCode(), clazz);
        return clazz;
    }

    private static Class<?> classForName(MessageEnvelope messageEnvelope) {
        try {
            final var clazz = Class.forName(messageEnvelope.payloadType());
            context.get().messageTypes.put(clazz.getName().hashCode(), clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> MessageEnvelope serialize(MutableDirectBuffer directBuffer, int offset, T message) throws SerializationException {
        final var encoding = resolveEncoding(message.getClass());
        try {
            final var length = serializeAndReturnLength(directBuffer, offset, message, encoding);
            return new MessageEnvelope(
                    encoding,
                    message.getClass().getName(),
                    offset,
                    length,
                    directBuffer
            );
        } catch (Exception e) {
            throw new SerializationException(encoding, message.getClass(), e);
        }

    }

    private static Encoding resolveEncoding(Class<?> messageClass) {
        return context.get().encodingCache.getOrDefault(messageClass.getName().hashCode(), encoding(messageClass));
    }

    private static Encoding encoding(Class<?> messageClass) {
        final var encoding = context.get().messageEncoding.get(messageClass.getName().hashCode());
        context.get().encodingCache.put(messageClass.getName().hashCode(), encoding);
        return encoding;
    }

    private static <T> int serializeAndReturnLength(MutableDirectBuffer directBuffer, int offset, T message, Encoding encoding) throws Exception {
        return switch (encoding) {
            case SBE -> ((DirectBufferCodec<T>) sbeEncoder(message.getClass().getName().hashCode()))
                    .encode(message, directBuffer, offset, null);
            case FURY -> context.get().fury.serialize(message, directBuffer, offset);
            case KYRO -> context.get().kyro.serialize(message, directBuffer, offset);
            case JSON -> context.get().json.serialize(message, directBuffer, offset);
            case NULL_VAL -> throw new IllegalArgumentException("Illegal encoder");
        };
    }

    private static DirectBufferCodec<?> sbeEncoder(int hashcode) {
        return context.get().sbeEncoderCache.getOrDefault(hashcode, resolveSbeEncoder(hashcode));
    }

    private static DirectBufferCodec<?> resolveSbeEncoder(int hashcode) {
        final var encoder = context.get().sbeEncoders.get(hashcode);
        context.get().sbeEncoderCache.put(hashcode, encoder);
        return encoder;
    }

}
