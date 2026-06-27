# 設計ドキュメント

> チーム名：（記入してください）  
> メンバー：（記入してください）

---

## 1. 課題の整理

スターターコードはパスワード登録だけを想定しているため、GitHub OAuth登録を追加するときに「どこで登録方法を選ぶか」を決める必要があります。

今回は大きな設計変更を避け、既存の `register` メソッドの流れを残したまま、メソッドの先頭で登録方法を選択します。

---

## 2. 設計方針

- `if` 文は `register` メソッドの最初、入力を受け取った直後かつ共通バリデーション・重複チェック・DB保存より前に置きます。
- 理由は、登録方法によって必要な入力が違うためです。パスワード登録ではパスワードを検証し、GitHub登録では認可コードからメールアドレス・名前を取得してから共通処理に進みます。
- GoogleやLINEを追加するときは、同じ `if` 文の登録方法選択部分に `google` / `line` の分岐を追加します。入力項目は `RegisterInput` に追加し、分岐内で共通処理に必要な `email`・`name`・`password` をそろえます。
- GitHub分岐では、認可コードをアクセストークンへ交換し、そのトークンでプロフィールを取得する流れをメソッドとして表現します。

---

## 3. クラス・メソッド構成

現在の最小構成です。

```
UserRegistrationService
└── register
    ├── 登録方法の選択 if (password / github / 将来 google・line / elseはエラー)
    │   ├── exchangeGitHubAuthorizationCodeForAccessToken
    │   └── fetchGitHubProfile
    ├── 共通バリデーション
    ├── 重複チェック
    ├── パスワードハッシュ化
    ├── DB保存
    ├── RegistrationCompletedMessage作成
    ├── 確認メール送信
    └── ログ記録
```

追加箇所の目安です。

- 登録方法の選択肢: `register` メソッド先頭の `if` 文に追加する。未知の値は `else` でエラーにする。
- 追加の入力値: `RegisterInput` にフィールド・getter・setterを追加する。
- 後続処理: DB保存後に `RegistrationCompletedMessage` を作成し、登録方法に関係なく `sendWelcomeEmail` と `recordRegistrationLog` に渡す。

---

## 4. 工夫したポイント

まずは小さく試すため、クラス分割やOAuthプロバイダー抽象化はまだ行っていません。

一方で、`if` 文はDB保存やメール送信の前に置いているため、GitHub登録でもパスワード登録と同じ確認メール・ログ記録を通れます。DB保存後は `RegistrationCompletedMessage` を作り、ウェルカムメール送信とログ記録を同じメソッドに集約しています。

---

## 5. できなかったこと・今後の改善点

- GitHub OAuth APIの実呼び出しはまだ実装していません。現在は `exchangeGitHubAuthorizationCodeForAccessToken` と `fetchGitHubProfile` に処理の置き場所だけを用意しています。
- GoogleやLINEを追加するときは、まず `if` 文に分岐を追加できます。分岐が増えて読みづらくなった時点で、OAuthプロバイダーごとの処理をクラス分割することを検討します。
- `signupMethod` は文字列ではなく enum にすると、タイプミスを減らせます。
