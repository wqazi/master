package lombok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HelloWorld
public class TestSubject {

	//public Logger log = null;
	//final static Logger log = LoggerFactory.getLogger(TestSubject.class);
	//private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("lombok.TestSubject");
//    public void helloWorld() {
//        System.out.println("Hello World");
//    }
	
	@LogMe(level="debug",name="waseeLog",printReturnValue=true)
	public String testMe(String s,String s1) {
		System.out.println("Hello");
		return "Wasee";
	}
}
