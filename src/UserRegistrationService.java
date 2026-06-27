package com.youtrust.hackathon;

import java.util.logging.Logger;

/**
 * ユーザー登録サービス（スターターコード）
 *
 * TODO: このクラスをリファクタリングしてください。
 * どう設計するか、なぜそう設計するかを DESIGN.md と DECISIONS.md に記録してください。
 */
public class UserRegistrationService {

    private static final Logger logger = Logger.getLogger(UserRegistrationService.class.getName());

    // データベース接続（簡略化のためモック）
    private final Database database = new Database();

    // メール送信（簡略化のためモック）
    private final EmailClient emailClient = new EmailClient();

    /**
     * ユーザーを登録する
     *
     * @param input 登録情報
     * @return 登録結果
     * @throws Exception 何か問題が起きたとき
     */
    public RegisterResult register(RegisterInput input) throws Exception {

        if (input == null) {
            throw new IllegalArgumentException("登録情報は必須です");
        }

        // 登録方法の選択
        if ("password".equals(input.getSignupMethod())) {
            // パスワード登録
            if (input.getPassword() == null || input.getPassword().length() < 8) {
                throw new IllegalArgumentException("パスワードは8文字以上必要です");
            }
        } else if ("github".equals(input.getSignupMethod())) {
            // GitHub OAuth（簡略化）
            // 1. 認可コードをアクセストークンに交換する
            // 2. アクセストークンでGitHubプロフィールを取得する
            // 3. 既存の登録処理に流せるように email / name / password をそろえる
            String accessToken = exchangeGitHubAuthorizationCodeForAccessToken(input.getGithubAuthorizationCode());
            GitHubProfile gitHubProfile = fetchGitHubProfile(accessToken);
            input.setEmail(gitHubProfile.getEmail());
            input.setName(gitHubProfile.getName());
            input.setPassword(null);
        // } else if ("google".equals(input.getSignupMethod())) {
        //     // 将来Googleログインを追加するときのテンプレート
        //     // String accessToken = exchangeGoogleAuthorizationCodeForAccessToken(input.getGoogleAuthorizationCode());
        //     // GoogleProfile googleProfile = fetchGoogleProfile(accessToken);
        //     // input.setEmail(googleProfile.getEmail());
        //     // input.setName(googleProfile.getName());
        //     // input.setPassword(null);
        // } else if ("line".equals(input.getSignupMethod())) {
        //     // 将来LINEログインを追加するときのテンプレート
        //     // String accessToken = exchangeLineAuthorizationCodeForAccessToken(input.getLineAuthorizationCode());
        //     // LineProfile lineProfile = fetchLineProfile(accessToken);
        //     // input.setEmail(lineProfile.getEmail());
        //     // input.setName(lineProfile.getName());
        //     // input.setPassword(null);
        } else {
            throw new IllegalArgumentException("未対応の登録方法です: " + input.getSignupMethod());
        }

        // バリデーション
        if (input.getEmail() == null || !input.getEmail().contains("@")) {
            throw new IllegalArgumentException("メールアドレスが無効です");
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("名前は必須です");
        }

        // 重複チェック
        if (database.findByEmail(input.getEmail()) != null) {
            throw new IllegalArgumentException("このメールアドレスはすでに登録されています");
        }

        // パスワードハッシュ化（OAuth登録ではパスワードなし）
        String hashedPassword = null;
        if (input.getPassword() != null) {
            hashedPassword = input.getPassword() + "_hashed";
        }

        // DBに保存
        User user = new User();
        user.setEmail(input.getEmail());
        user.setName(input.getName());
        user.setPassword(hashedPassword);
        database.save(user);

        // 登録完了メッセージを作成し、登録方法に関係なく同じ後続処理に渡す
        RegistrationCompletedMessage message = new RegistrationCompletedMessage(
                user.getEmail(),
                user.getName(),
                input.getSignupMethod());

        // 確認メール送信
        sendWelcomeEmail(message);

        // ログ記録
        recordRegistrationLog(message);

        return new RegisterResult(true, user.getId(), "登録が完了しました");
    }

    private void sendWelcomeEmail(RegistrationCompletedMessage message) {
        String subject = "【ハッカソン】登録完了のお知らせ";
        String body = message.getName() + " 様\n\nご登録ありがとうございます。";
        emailClient.send(message.getEmail(), subject, body);
    }

    private void recordRegistrationLog(RegistrationCompletedMessage message) {
        logger.info("ユーザー登録完了: " + message.getEmail() + " signupMethod=" + message.getSignupMethod());
    }

    private String exchangeGitHubAuthorizationCodeForAccessToken(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub認可コードは必須です");
        }

        // 実運用では GitHub の token API に client_id / client_secret / code を送って交換する。
        return "github_access_token_" + authorizationCode.trim();
    }

    private GitHubProfile fetchGitHubProfile(String accessToken) {
        // 実運用では GitHub の user API と emails API を呼び、検証済みprimary emailを使う。
        String githubUserId = accessToken.replace("github_access_token_", "");
        return new GitHubProfile(githubUserId + "@users.noreply.github.com", "GitHub User");
    }


    // ---- 以下はモッククラス（変更不要） ----

    static class Database {
        public User findByEmail(String email) { return null; }
        public void save(User user) { user.setId("user_" + System.currentTimeMillis()); }
    }

    static class EmailClient {
        public void send(String to, String subject, String body) {
            System.out.println("Email sent to: " + to);
        }
    }

    static class RegistrationCompletedMessage {
        private final String email;
        private final String name;
        private final String signupMethod;
        public RegistrationCompletedMessage(String email, String name, String signupMethod) {
            this.email = email;
            this.name = name;
            this.signupMethod = signupMethod;
        }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getSignupMethod() { return signupMethod; }
    }

    static class GitHubProfile {
        private final String email;
        private final String name;
        public GitHubProfile(String email, String name) {
            this.email = email;
            this.name = name;
        }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }

    static class User {
        private String id;
        private String email;
        private String name;
        private String password;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class RegisterInput {
        private String email;
        private String password;
        private String name;
        private String signupMethod = "password";
        private String githubAuthorizationCode;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSignupMethod() { return signupMethod; }
        public void setSignupMethod(String signupMethod) { this.signupMethod = signupMethod; }
        public String getGithubAuthorizationCode() { return githubAuthorizationCode; }
        public void setGithubAuthorizationCode(String githubAuthorizationCode) { this.githubAuthorizationCode = githubAuthorizationCode; }
    }

    static class RegisterResult {
        private final boolean success;
        private final String userId;
        private final String message;
        public RegisterResult(boolean success, String userId, String message) {
            this.success = success;
            this.userId = userId;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getUserId() { return userId; }
        public String getMessage() { return message; }
    }
}
