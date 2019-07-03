package simple_service.restful_service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController {
    private static final String template = "%s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value={"/service-instances/{applicationName}","/service-instances/ows","/service-instances/wms"})
    public SimpleResponse simpleResponse(@RequestParam(value="content", defaultValue="ArgleBargle") String content) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            //Do nothing
        }
        return new SimpleResponse(counter.incrementAndGet(),
                String.format(template, content));
    }
}
