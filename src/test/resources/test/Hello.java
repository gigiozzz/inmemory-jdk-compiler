package test;

import com.gigiozzz.inmemory.jdk.compiler.TestInterface;
import com.google.common.collect.ImmutableList;
import java.util.Map;

public class Hello implements TestInterface {

    public static void sayHello(String name) {
        System.out.println("Hello " + name);
    }

    public Object run(Map parameters) {
        if (parameters.containsKey("name")) {
            Hello.sayHello(parameters.get("name").toString());
        }
        ImmutableList.builder();
        System.out.println("Hello World system out");
        System.out.println("String function isBlank: "+ ("".isBlank()));
        return "Hello " + parameters.get("name");
    }
}
