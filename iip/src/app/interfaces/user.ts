export interface User {
    user_id?: number;
    exp?: number;
    iat?: number;
    jti?: string;
    token?: string;
    token_type?: string;
    refreshToken?: string;
}