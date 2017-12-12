package redis.clients.util;

import redis.clients.jedis.exceptions.InvalidProxyException;

import java.io.UnsupportedEncodingException;
import java.net.*;

public final class JedisURIHelper {

  private static final int DEFAULT_DB = 0;

  private JedisURIHelper(){
    throw new InstantiationError( "Must not instantiate this class" );
  }

  public static String getPassword(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      return userInfo.split(":", 2)[1];
    }
    return null;
  }

  public static int getDBIndex(URI uri) {
    String[] pathSplit = uri.getPath().split("/", 2);
    if (pathSplit.length > 1) {
      String dbIndexStr = pathSplit[1];
      if (dbIndexStr.isEmpty()) {
        return DEFAULT_DB;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return DEFAULT_DB;
    }
  }

  public static boolean isValid(URI uri) {
    if (isEmpty(uri.getScheme()) || isEmpty(uri.getHost()) || uri.getPort() == -1) {
      return false;
    }

    return true;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().length() == 0;
  }

  public static Proxy getProxy(URI uri) {
    String query = uri.getQuery();
    if(query ==null) return null;
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      String[] keyValue=pair.split("=",2);
      if(keyValue.length > 1 &&  "proxy".equalsIgnoreCase(keyValue[0] )){
        try {
            String proxyStr=URLDecoder.decode(keyValue[1] , "UTF-8");
            URI proxyUri = new URI(proxyStr);
            return  getProxyFromUri(proxyUri);
        } catch (UnsupportedEncodingException e) {
          String message = "UnsupportedEncoding for proxy setting:"+ pair;
          throw new InvalidProxyException( message,e);
        } catch (URISyntaxException e) {
            String message = String.format("proxy synctax invalid  : [%s] , please check",pair);
            throw  new InvalidProxyException(message, e);
        }catch (Exception e){
            throw  new InvalidProxyException("proxy parse failure,please check proxy synctax or new issue",e);
        }
      }

    }
    return null;
  }

  private static Proxy  getProxyFromUri(URI uri) throws UnsupportedEncodingException {
    if("socks5".equalsIgnoreCase(uri.getScheme() )){
        String host = uri.getHost();
        int port = uri.getPort();
        String  user =uri.getUserInfo();
        if(user !=null && user.length()>0){
            String[] pair = user.split(":",2);
            if(pair !=null && pair.length >0){
               final String userName =URLDecoder.decode(pair[0],"UTF-8");
               final String pass = URLDecoder.decode(pair[1],"UTF-8");
               Authenticator.setDefault(new Authenticator(){
                  @Override
                        protected PasswordAuthentication getPasswordAuthentication(){
                             return new PasswordAuthentication(userName, pass.toCharArray());
                  }
              });
            }
        }
        InetSocketAddress socketAddress = new InetSocketAddress(host,port);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socketAddress);
        return  proxy;
    }
    return  null;
  }
}
