package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface LogMe {
    String level() default "info";
    String name() default "log";
    boolean printReturnValue() default false;
}
