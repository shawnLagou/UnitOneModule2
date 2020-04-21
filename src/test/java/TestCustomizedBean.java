
import com.shawn.dao.AccountDao;
import com.shawn.factory.BeanFactory;
import com.shawn.factory.ProxyFactory;
import com.shawn.service.TransferService;
import com.shawn.utils.ComponentScanUtils;
import org.junit.Test;


public class TestCustomizedBean {

    @Test
    public void testCustomizedIoc() throws Exception {
        ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("ProxyFactory");
        TransferService transferService = (TransferService) proxyFactory.getJdkProxy(BeanFactory.getBean("TransferService")) ;
        transferService.transfer("6029621011001", "6029621011000", 2000);

        AccountDao accountDao = (AccountDao) BeanFactory.getBean("123");
        System.out.println(accountDao);
    }

    @Test
    public void testTransactional() throws Exception {
        ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("ProxyFactory");
        TransferService transferService = null;
        transferService = (TransferService) BeanFactory.getBean("TransferService");
        if (ComponentScanUtils.Scan("TransferService")) {
            if (transferService.getClass().getInterfaces().length > 0) {
                transferService = (TransferService) proxyFactory.getJdkProxy(transferService);
            } else {
                transferService = (TransferService) proxyFactory.getCglibProxy(transferService);
            }
        }
        transferService.transfer("6029621011001", "6029621011000", 2000);
    }
}
