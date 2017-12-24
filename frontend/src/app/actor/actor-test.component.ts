import {  Component } from '@angular/core';

import { ActorService } from './actor.service';
import { Address } from './address';
import { Message } from './message';
import { OnChanges } from '@angular/core/src/metadata/lifecycle_hooks';

// https://stackoverflow.com/a/45367387/1196226 -- autoscrolling recv msgs use this
@Component({
    selector: 'rfm-actor-test',
    template: `
        <pre>{{self}}</pre>
        <textarea rows="15" cols="85" wrap="off" [(ngModel)]="sendMsgs"></textarea>
        <button (click)='send()'>Send</button>
        <button (click)="clearSendMsgs()">Clear Send</button>
        <br>
        <br>
        <textarea rows="15" cols="85" wrap="off" readonly [(ngModel)]="recvMsgs" #scrollMe [scrollTop]="scrollMe.scrollHeight"></textarea>
        <button (click)="clearRecvMsgs()">Clear Recv</button>`
})
export class ActorTestComponent {
    private self: Address;
    private sendMsgs: string;
    private recvMsgs: string;

    constructor(private actorService: ActorService) {
        this.clearSendMsgs();
        this.clearRecvMsgs();
        this.actorService.ready((err, self) => {
            this.self = self;
            this.clearSendMsgs();
        });

        this.actorService.messageEmitter().on('servlet', (m) => {
            const recvMsgs: Array<any> = <Array<any>> JSON.parse(this.recvMsgs);

            const simplifiedMessage: any = this.toSimplified(m);
            recvMsgs.push(simplifiedMessage);

            this.recvMsgs = JSON.stringify(recvMsgs, undefined, 2);
        });
    }

    private clearSendMsgs(): void {
        if (this.self === undefined) {
            this.sendMsgs = '';
            return;
        }

        const templateMsg: Message = new Message(
            this.self,
            Address.fromString('actor:echoer'),
            'java.lang.String',
            'testmsg');
        const simplifiedTemplateMsg: any = this.toSimplified(templateMsg);
        this.sendMsgs = JSON.stringify([simplifiedTemplateMsg], undefined, 2);
    }

    private clearRecvMsgs(): void {
        this.recvMsgs = '[]';
    }

    private send(): void {
        const messages: Message[] = (<any[]> JSON.parse(this.sendMsgs)).map(m => this.fromSimplified(m));
        messages.forEach(msg => this.actorService.writeMessage(msg.source, msg.destination, msg.type, msg.data));
    }



    private fromSimplified(message: any): Message {
        return new Message(
            Address.fromString(message.source),
            Address.fromString(message.destination),
            message.type,
            message.data);
    }

    private toSimplified(message: Message): any {
        return {
            source: message.source.toString(),
            destination: message.destination.toString(),
            type: message.type,
            data: message.data
        };
    }
}
