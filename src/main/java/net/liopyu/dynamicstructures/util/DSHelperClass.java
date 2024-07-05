package net.liopyu.dynamicstructures.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.mojang.text2speech.Narrator.LOGGER;
public class DSHelperClass {
    public static final Set<String> errorMessagesLogged = new HashSet<>();
    public static final Set<String> warningMessagesLogged = new HashSet<>();
    public static void logErrorMessageOnce(String errorMessage) {
        if (!errorMessagesLogged.contains(errorMessage)) {
            LOGGER.error(errorMessage);
            errorMessagesLogged.add(errorMessage);
        }
    }
    public static void logErrorMessage(String errorMessage) {
        LOGGER.error(errorMessage);
    }
    public static void logWarningMessageOnce(String errorMessage) {
        if (!warningMessagesLogged.contains(errorMessage)) {
            LOGGER.warn(errorMessage);
            warningMessagesLogged.add(errorMessage);
        }
    }
    public static void logErrorMessageCatchable(String errorMessage, Throwable e) {
        LOGGER.error(errorMessage, e);
    }
    public static void logErrorMessageOnceCatchable(String errorMessage, Throwable e) {
        if (!errorMessagesLogged.contains(errorMessage)) {
            LOGGER.error(errorMessage, e);
            errorMessagesLogged.add(errorMessage);
        }
    }
    public static void logInfoMessage(String info) {
            LOGGER.info(info);
    }
    public static <T> boolean consumerCallback(Consumer<T> consumer, T value, String errorMessage) {
        try {
            consumer.accept(value);
        } catch (Throwable e) {
            logErrorMessageOnceCatchable(errorMessage, e);
            return false;
        }
        return true;
    }
    public static Object convertObjectToDesired(Object input, String outputType) {
        return switch (outputType.toLowerCase()) {
            case "integer" -> convertToInteger(input);
            case "double" -> convertToDouble(input);
            case "float" -> convertToFloat(input);
            case "boolean" -> convertToBoolean(input);
            case "interactionresult" -> convertToInteractionResult(input);
            case "resourcelocation" -> convertToResourceLocation(input);
            default -> input;
        };
    }
    public static ResourceLocation convertToResourceLocation(Object input) {
        if (input instanceof ResourceLocation) {
            return (ResourceLocation) input;
        } else if (input instanceof String) {
            return new ResourceLocation((String) input);
        }
        return null;
    }

    public static InteractionResult convertToInteractionResult(Object input) {
        if (input instanceof InteractionResult) {
            return (InteractionResult) input;
        } else if (input instanceof String) {
            String stringValue = ((String) input).toLowerCase();
            switch (stringValue) {
                case "success":
                    return InteractionResult.SUCCESS;
                case "consume":
                    return InteractionResult.CONSUME;
                case "pass":
                    return InteractionResult.PASS;
                case "fail":
                    return InteractionResult.FAIL;
                case "consume_partial":
                    return InteractionResult.CONSUME_PARTIAL;
            }
        }
        return null;
    }

    public static Boolean convertToBoolean(Object input) {
        if (input instanceof Boolean) {
            return (Boolean) input;
        } else if (input instanceof String) {
            String stringValue = ((String) input).toLowerCase();
            if ("true".equals(stringValue)) {
                return true;
            } else if ("false".equals(stringValue)) {
                return false;
            }
        }
        return null;
    }
    public static Integer convertToInteger(Object input) {
        if (input instanceof Integer) {
            return (Integer) input;
        } else if (input instanceof Double || input instanceof Float) {
            return ((Number) input).intValue();
        } else {
            return null;
        }
    }
    public static Double convertToDouble(Object input) {
        if (input instanceof Double) {
            return (Double) input;
        } else if (input instanceof Integer || input instanceof Float) {
            return ((Number) input).doubleValue();
        } else {
            return null;
        }
    }
    public static Float convertToFloat(Object input) {
        if (input instanceof Float) {
            return (Float) input;
        } else if (input instanceof Integer || input instanceof Double) {
            return ((Number) input).floatValue();
        } else {
            return null;
        }
    }
}