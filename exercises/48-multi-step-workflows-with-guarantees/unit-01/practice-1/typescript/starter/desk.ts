// A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md.
type Backend = Record<string, (args: any) => Record<string, any>>;

export class RefundDesk {
  private backend: Backend;
  private limitCents: number;

  constructor(backend: Backend, limitCents = 10000) {
    this.backend = backend;
    this.limitCents = limitCents;
    // TODO: keep what the desk has verified, looked up, refunded and blocked.
  }

  state(): any {
    // TODO: a snapshot of the desk's state, as described in the statement.
    return null;
  }

  call(name: string, args: any): any {
    // TODO: check the prerequisites in code, then call the backend, and return { content, is_error, blocked }.
    return null;
  }

  handoff(reason: string): any {
    // TODO: the structured hand-off a person needs to take the case over.
    return null;
  }
}
