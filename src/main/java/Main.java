import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws Exception {

        // 2種類のDI（依存性注入）コンテナを用意
        var consoleContainer =
            new Container()
            .register(ILog.class, ConsoleLog.class);
        var fileContainer =
            new Container()
            .register(ILog.class, FileLog.class);

        // それぞれLogicAインスタンスに具象クラスを注入
        var a = consoleContainer.resolve(new LogicA());
        a.doAny();

        var b = fileContainer.resolve(new LogicA());
        b.doAny();

        // ⇒例では1種類の差し替えだが、
        //   テスト環境と本番環境でコンテナを用意すれば
        //   複数のコンパーネントをコンテナ単位でまとめて管理できる
    }
}


// 機能を外部から登録（設定）するための
// 目印となるアノテーション
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Inject{}

class LogicA{
    // ログ機能を外部から設定する
    @Inject
    ILog log;

    void doAny(){
        log.write("doAnyLog");
        // …
    }
}

// ログ機能インタフェース
interface ILog{
    void write(String msg);
}

// コンソール出力するログ機能
class ConsoleLog implements ILog{
    public void write(String msg){
        System.out.println(msg);
    }
}

// ファイル出力するログ機能
class FileLog implements ILog{    
    public void write(String msg) {
        try{
            PrintWriter file = 
                new PrintWriter(new File("./Log.txt"));
            file.println(msg); 
            file.close();   
        }catch(Exception e){}
    }
}

class Container{
    Map<Type, Class<?>> dic =
        new HashMap<Type, Class<?>>();

    /** インタフェースと対応する実体クラスを登録する */
    <I, C extends I> 
    Container register(Class<I> intface,
                       Class<C> clss){
        dic.put(intface, clss);
        return this;
    }

    /** 依存関係の注入対象のフィールド(@Inject)に
     *  具象クラスの実態を設定する    */
    <T> T resolve(T obj) throws Exception{
        Field[] injectFields = 
        Stream.of(obj.getClass().getDeclaredFields())
              .filter(x -> x.getAnnotation(Inject.class) != null)
              .toArray(Field[]::new);

        for(Field f: injectFields){
            Object val = dic.get(f.getType())
                         .getDeclaredConstructor()
                         .newInstance();
            f.set(obj, val);
        }
        // 本当は再帰的に解決しないといけない
        return obj;
    }
}



