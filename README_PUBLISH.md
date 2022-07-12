1. 发布，参考文章：https://juejin.cn/post/6844904185754812423
1.1 发布到local maven(.m2目录): ./gradlew :pioneer-gradle-plugin:publishToMavenLocal
1.2 发布到remote maven或者指定目录: ./gradlew :pioneer-gradle-plugin:publish


2. 发布到jcenter，在项目根目录依次输入下面两个命令
2.1 ./gradlew install
2.2 ./gradlew bintrayUpload

参考文章： https://blog.csdn.net/linglongxin24/article/details/53415932
还有一篇宣传极简文章：https://juejin.im/post/59cef9baf265da066a105f92
也可以像arouter那样把install和bintray拆封成两个文件install.gradle和bintray.gradle
butterknife中对android和java包做了区分，值得参考 https://github.com/JakeWharton/butterknife/blob/master/gradle/gradle-mvn-push.gradle
