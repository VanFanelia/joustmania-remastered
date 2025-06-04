export enum ApiStatus {
    OK,
    ERROR,
}

export type ApiResult =
    | { status: ApiStatus.OK }
    | { status: ApiStatus.ERROR, reason: string }
