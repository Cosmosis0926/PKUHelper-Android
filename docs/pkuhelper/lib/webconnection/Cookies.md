public class
## Cookies
***
com.pkuhelper.lib.webconnection.Cookies

### Class Overview
һ���������ú����cookies�Ĺ����ࡣ
����һ��˽�о�̬��Ա����**cookies**����Key-Value��ʽ�洢������cookies���ݡ�

### Public Methods
> public static void **setCookie**(HttpResponse response, String url)

��response�С�Set-Cookie��Ӧ��ͷ������Key-Value��ʽ��ʽ������ȡurl���ڵ�����������������cookies��Key-Value��ʽ�ݴ��ڱ����ڴ档
- **Parameters**
	- *response* - cookies��Դ��
	- *url* - cookies�����ݴ���*url*���ڵ�������Key�¡�

***
> public static void **addCookie**(HttpRequestBase httpRequestBase)

�������ݴ��cookies����Http�����header��ͷ��
- **Parameters**
	- *httpRequestBase* - ��������cookies��Http����