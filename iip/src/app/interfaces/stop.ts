export interface Stop {
    stop_id: string;
    direction?: string;
    lat?: number;
    lng?: number;
    stops?: Array<string>;
    name?: string;
    info?: string;
    street?: string;
    distance?: number;
}
